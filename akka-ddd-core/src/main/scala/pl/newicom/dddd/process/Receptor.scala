package pl.newicom.dddd.process

import akka.actor.ActorPath
import akka.contrib.pattern.ReceivePipeline
import akka.persistence.PersistentActor
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.delivery.AtLeastOnceDeliverySupport
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.{DemandConfig, DemandCallback}
import pl.newicom.dddd.messaging.event._
import pl.newicom.dddd.messaging.{Message, MetaData}
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.process.ReceptorConfig.{ReceiverResolver, StimuliSource, Transduction}
import pl.newicom.dddd.persistence.{RegularSnapshottingConfig, RegularSnapshotting}

object ReceptorConfig {
  type Transduction = PartialFunction[EventMessage, Message]
  type ReceiverResolver = PartialFunction[Message, ActorPath]
  type StimuliSource = BusinessEntity
}

case class ReceptorConfig(
   stimuliSource: StimuliSource,
   transduction: Transduction,
   receiverResolver: ReceiverResolver,
   capacity: Int
)

trait ReceptorGrammar {
  def reactTo[A : LocalOfficeId]:                                     ReceptorGrammar
  def applyTransduction(transduction: Transduction):                  ReceptorGrammar
  def route(receiverResolver: ReceiverResolver):                      ReceptorConfig
  def propagateTo(receiver: ActorPath):                               ReceptorConfig
}

case class ReceptorBuilder(
    stimuliSource:    StimuliSource = null,
    transduction:     Transduction = {case em => em},
    receiverResolver: ReceiverResolver = null,
    capacity:         Int = 1000)
  extends ReceptorGrammar {

  def reactTo[A : LocalOfficeId]: ReceptorBuilder = {
    reactTo(implicitly[LocalOfficeId[A]].asInstanceOf[BusinessEntity])
  }

  def reactTo(observable: BusinessEntity): ReceptorBuilder = {
    copy(stimuliSource = observable)
  }

  def applyTransduction(transduction: Transduction): ReceptorBuilder =
    copy(transduction = transduction)

  def route(receiverResolver: ReceiverResolver): ReceptorConfig =
    ReceptorConfig(stimuliSource, transduction, receiverResolver, capacity)

  def propagateTo(receiver: ActorPath): ReceptorConfig =
    route({case _ => receiver})

  def withCapacity(capacity: Int): ReceptorBuilder =
    copy(capacity = capacity)
}

trait ReceptorPersistence extends ReceivePipeline with RegularSnapshotting {
  this: PersistentActor =>

  override def journalPluginId = "akka.persistence.journal.inmem"

}

abstract class Receptor extends AtLeastOnceDeliverySupport with ReceptorPersistence {
  this: EventStreamSubscriber =>

  def config: ReceptorConfig

  val snapshottingConfig = RegularSnapshottingConfig(
    interest = receiveEvent,
    interval = config.capacity) // TODO: snapshoting interval should be configured independently of the receptor capacity ?

  def deadLetters = context.system.deadLetters.path

  def destination(msg: Message): ActorPath =
    config.receiverResolver.applyOrElse(msg, (any: Message) => deadLetters)

  override lazy val persistenceId: String =
    s"Receptor-${config.stimuliSource.id}-${self.path.hashCode}"

  var demandCallback: Option[DemandCallback] = None

  override def recoveryCompleted(): Unit = {
    demandCallback = Some(subscribe(
        observable            = config.stimuliSource,
        fromPositionExclusive = lastSentDeliveryId,
        demandConfig          = DemandConfig(
                                  subscriberCapacity = config.capacity,
                                  initialDemand = config.capacity - unconfirmedNumber)))
    log.info(s"$persistenceId subscribed to '${config.stimuliSource.id}' event stream from position: ${lastSentDeliveryId.getOrElse(0)}.")
  }


  override def receiveCommand: Receive =
    receiveEvent.orElse(deliveryStateReceive).orElse {
      case other =>
        log.warning(s"RECEIVED: $other")
    }

  def receiveEvent: Receive = {
    case EventMessageEntry(em, position, _) =>
      val msgToDeliver = em.withMetaData(metaDataProvider(em))
      config.transduction.lift(msgToDeliver).foreach { msg =>
        deliver(msg, deliveryId = position)
      }
  }

  override def deliveryConfirmed(deliveryId: Long): Unit =
    demandCallback.foreach(_.onEventProcessed())

  def metaDataProvider(em: OfficeEventMessage): Option[MetaData] = None

}
