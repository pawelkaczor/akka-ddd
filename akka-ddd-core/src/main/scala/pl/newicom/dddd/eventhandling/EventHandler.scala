package pl.newicom.dddd.eventhandling

import akka.actor.ActorRef
import pl.newicom.dddd.messaging.event.DomainEventMessage

trait EventHandler {
  def handle(senderRef: ActorRef, event: DomainEventMessage)
}
