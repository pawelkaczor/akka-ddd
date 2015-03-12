package pl.newicom.dddd.process

import akka.actor.ActorPath
import pl.newicom.dddd.delivery.AtLeastOnceDeliverySupport
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}
import pl.newicom.dddd.messaging.{Message, MetaData}
import pl.newicom.dddd.serialization.JsonSerializationHints

class ReceptorConfig[A : JsonSerializationHints](val stimuliSource: String) {
  def serializationHints: JsonSerializationHints[A] = implicitly[JsonSerializationHints[A]]
}

abstract class Receptor(config: ReceptorConfig[_], receiver: ActorPath) extends AtLeastOnceDeliverySupport {
  this: EventStreamSubscriber =>

  override val destination = receiver
  override def persistenceId: String = s"Receptor-${config.stimuliSource}"

  def outgoingMessage: PartialFunction[EventMessage, Message]

  override def recoveryCompleted(): Unit =
    subscribe(config.stimuliSource, lastSentDeliveryId)

  override def receiveCommand: Receive =
    receiveEvent(metaDataProvider).orElse(deliveryStateReceive).orElse {
      case other =>
        log.warning(s"RECEIVED: $other")
    }

  override def eventReceived(em: EventMessage, position: Long): Unit =
    outgoingMessage.lift(em).foreach { msg =>
      deliver(msg, deliveryId = position)
    }

  def metaDataProvider(em: EventMessage): Option[MetaData] = None

}
