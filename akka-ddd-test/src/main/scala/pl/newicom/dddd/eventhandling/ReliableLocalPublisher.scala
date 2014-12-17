package pl.newicom.dddd.eventhandling

import pl.newicom.dddd.aggregate.DomainEvent

class ReliableLocalPublisher extends ReliableEventHandler {
  override def handle(event: DomainEvent) = context.system.eventStream.publish(event)
}
