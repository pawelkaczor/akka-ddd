package pl.newicom.dddd.messaging.event

case class EventMessageRecord(msg: DomainEventMessage, position: Long)
