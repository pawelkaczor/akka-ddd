package pl.newicom.dddd.delivery.protocol

import pl.newicom.dddd.aggregate.DomainEvent

case class ViewUpdated(event: DomainEvent) extends Receipt
