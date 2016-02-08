package pl.newicom.dddd.aggregate

import pl.newicom.dddd.messaging.event.EventMessage

trait EventMessageFactory {
  def toEventMessage(event: DomainEvent): EventMessage = EventMessage(event)
}
