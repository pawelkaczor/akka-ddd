package pl.newicom.dddd.eventhandling

import pl.newicom.dddd.messaging.event.DomainEventMessage

trait EventHandler {
  def handle(event: DomainEventMessage)
}
