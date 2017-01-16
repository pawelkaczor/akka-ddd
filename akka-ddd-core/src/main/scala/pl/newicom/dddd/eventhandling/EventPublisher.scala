package pl.newicom.dddd.eventhandling

import akka.actor.ActorRef
import pl.newicom.dddd.messaging.event.OfficeEventMessage

trait EventPublisher extends EventHandler {

  override abstract def handle(senderRef: ActorRef, events: Seq[OfficeEventMessage]): Unit = {
    events.foreach(publish)
    super.handle(senderRef, events)
  }

  def publish(event: OfficeEventMessage)
}
