package pl.newicom.dddd.aggregate

import pl.newicom.dddd.messaging.PublisherType
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.OfficeId

trait EventMessageFactory {
  def toEventMessage(event: DomainEvent, source: OfficeId): EventMessage =
    EventMessage(event)
      .withPublisherType(PublisherType.AR)
      .withTags(source.id, source.department)
}
