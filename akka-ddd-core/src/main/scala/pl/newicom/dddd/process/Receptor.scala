package pl.newicom.dddd.process

import akka.actor.ActorPath
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.AtLeastOnceDeliverySupport
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}
import pl.newicom.dddd.serialization.JsonSerializationHints

import scala.concurrent.duration._

abstract class ReceptorConfig[A <: BusinessEntity : JsonSerializationHints] {
  def stimuliSource: String
  def serializationHints: JsonSerializationHints[A] = implicitly[JsonSerializationHints[A]]
}

abstract class Receptor(config: ReceptorConfig[_], receiver: ActorPath) extends AtLeastOnceDeliverySupport {
  this: EventStreamSubscriber =>

  override val destination = receiver
  override def persistenceId: String = s"Receptor-${config.stimuliSource}"
  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  override def recoveryCompleted(): Unit =
    subscribe(config.stimuliSource, lastSentDeliveryId)

  override def receiveCommand: Receive =
    receiveEvent(metaDataProvider).orElse(deliveryStateReceive).orElse {
      case other =>
        log.warning(s"RECEIVED: $other")
    }

  override def eventReceived(em: EventMessage, position: Long): Unit =
    persist(em, deliveryId = position)

  protected def metaDataProvider(em: EventMessage): Option[MetaData] = None

}
