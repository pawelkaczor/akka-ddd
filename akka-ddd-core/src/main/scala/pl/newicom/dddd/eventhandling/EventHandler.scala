package pl.newicom.dddd.eventhandling

import akka.actor.ActorRef
import pl.newicom.dddd.messaging.event.OfficeEventMessage

trait EventHandler {
  def handle(senderRef: ActorRef, event: OfficeEventMessage)
}
