package pl.newicom.dddd.eventhandling

import akka.actor.ActorRef
import pl.newicom.dddd.messaging.event.DomainEventMessage

trait EventPublisher extends EventHandler {

  override abstract def handle(senderRef: ActorRef, event: DomainEventMessage): Unit = {
    publish(event)
    super.handle(senderRef, event)
  }

  def publish(event: DomainEventMessage)
}
