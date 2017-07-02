package pl.newicom.dddd.aggregate

import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.OfficeId

trait EventMessageFactory {
  def toEventMessage(event: DomainEvent, source: OfficeId): EventMessage =
    EventMessage(event).withTags(source.id, source.department)
}
