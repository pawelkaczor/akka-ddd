package pl.newicom.dddd.process

import akka.actor.ActorPath
import akka.contrib.pattern.ReceivePipeline
import akka.persistence.PersistentActor
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.delivery.{DeliveryState, AtLeastOnceDeliverySupport}
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.{InFlightMessagesCallback, EventReceived}
import pl.newicom.dddd.messaging.event._
import pl.newicom.dddd.messaging.{Message, MetaData}
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.process.ReceptorConfig.{ReceiverResolver, StimuliSource, Transduction}
import pl.newicom.dddd.persistence.{RegularSnapshottingConfig, RegularSnapshotting, ForgettingParticularEvents}

object ReceptorConfig {
  type Transduction = PartialFunction[EventMessage, Message]
  type ReceiverResolver = PartialFunction[Message, ActorPath]
  type StimuliSource = BusinessEntity
}

abstract class ReceptorConfig {
  def stimuliSource: StimuliSource
  def transduction: Transduction
  def receiverResolver: ReceiverResolver
}

trait ReceptorGrammar {
  def reactTo[A : LocalOfficeId]:                                     ReceptorGrammar
  def applyTransduction(transduction: Transduction):                  ReceptorGrammar
  def route(receiverResolver: ReceiverResolver):                      ReceptorConfig
  def propagateTo(receiver: ActorPath):                               ReceptorConfig
}

case class ReceptorBuilder(
    stimuliSource: StimuliSource = null,
    transduction: Transduction = {case em => em},
    receiverResolver: ReceiverResolver = null)
  extends ReceptorGrammar {

  def reactTo[A : LocalOfficeId]: ReceptorBuilder = {
    reactTo(implicitly[LocalOfficeId[A]].asInstanceOf[BusinessEntity])
  }

  def reactTo(observable: BusinessEntity): ReceptorBuilder = {
    copy(stimuliSource = observable)
  }

  def applyTransduction(transduction: Transduction) =
    copy(transduction = transduction)

  def route(_receiverResolver: ReceiverResolver) =
    new ReceptorConfig() {
      def stimuliSource = ReceptorBuilder.this.stimuliSource
      def transduction = ReceptorBuilder.this.transduction
      def receiverResolver = _receiverResolver
    }

  def propagateTo(_receiver: ActorPath) = route({case _ => _receiver})
}

trait ReceptorPersistencePolicy extends ReceivePipeline with ForgettingParticularEvents with RegularSnapshotting {
  this: PersistentActor =>
}

abstract class Receptor extends AtLeastOnceDeliverySupport with ReceptorPersistencePolicy {
  this: EventStreamSubscriber =>

  def config: ReceptorConfig

  val snapshottingConfig = RegularSnapshottingConfig(receiveEvent, 1000)

  def deadLetters = context.system.deadLetters.path

  def destination(msg: Message): ActorPath =
    config.receiverResolver.applyOrElse(msg, (any: Message) => deadLetters)

  override lazy val persistenceId: String =
    s"Receptor-${config.stimuliSource.id}-${self.path.hashCode}"

  var inFlightCallback: Option[InFlightMessagesCallback] = None

  override def recoveryCompleted(): Unit =
    inFlightCallback = Some(subscribe(config.stimuliSource, lastSentDeliveryId))

  override def receiveCommand: Receive =
    receiveEvent.orElse(deliveryStateReceive).orElse {
      case other =>
        log.warning(s"RECEIVED: $other")
    }

  def receiveEvent: Receive = {
    case EventReceived(em, position) =>
      config.transduction.lift(em).foreach { msg =>
        deliver(msg, deliveryId = position)
      }
  }

  override def deliveryStateUpdated(deliveryState: DeliveryState): Unit =
    inFlightCallback.foreach(_.onChanged(deliveryState.unconfirmedNumber))

  override def metaDataProvider(em: EventMessage): Option[MetaData] = None

}
