package pl.newicom.dddd.delivery.protocol

import pl.newicom.dddd.aggregate.AggregateRoot.DomainEvent

case class ViewUpdated(event: DomainEvent) extends Receipt
