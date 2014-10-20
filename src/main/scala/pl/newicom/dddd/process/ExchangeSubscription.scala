package pl.newicom.dddd.process

import pl.newicom.dddd.aggregate.DomainEvent

case class ExchangeSubscription(exchangeName: String, events: Class[DomainEvent]*)
