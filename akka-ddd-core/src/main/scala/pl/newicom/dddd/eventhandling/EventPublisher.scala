package pl.newicom.dddd.eventhandling

import akka.actor.ActorRef
import pl.newicom.dddd.messaging.event.OfficeEventMessage

trait EventPublisher extends EventHandler {

  override abstract def handle(senderRef: ActorRef, event: OfficeEventMessage): Unit = {
    publish(event)
    super.handle(senderRef, event)
  }

  def publish(event: OfficeEventMessage)
}
