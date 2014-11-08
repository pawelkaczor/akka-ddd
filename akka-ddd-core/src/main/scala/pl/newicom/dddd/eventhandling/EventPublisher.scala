package pl.newicom.dddd.eventhandling

import pl.newicom.dddd.messaging.event.DomainEventMessage

trait EventPublisher extends EventHandler {

  override abstract def handle(event: DomainEventMessage): Unit = {
    publish(event)
    super.handle(event)
  }

  def publish(event: DomainEventMessage)
}
