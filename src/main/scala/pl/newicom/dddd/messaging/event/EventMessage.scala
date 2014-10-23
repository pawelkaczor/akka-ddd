package pl.newicom.dddd.messaging.event

import java.util.{Date, UUID}

import pl.newicom.dddd.aggregate.AggregateRoot.DomainEvent
import pl.newicom.dddd.messaging.Message
import pl.newicom.dddd.messaging.Message.MetaData

object EventMessage {
  def unapply(em: EventMessage): Option[(String, DomainEvent)] = {
    Some(em.identifier, em.event)
  }
}
class EventMessage(
    val event: DomainEvent,
    val identifier: String = UUID.randomUUID().toString,
    val timestamp: Date = new Date,
    val metaData: Option[MetaData] = None)
  extends Message(metaData) {

  override def toString: String = {
    val msgClass = getClass.getSimpleName
    s"$msgClass(event = $event, identifier = $identifier, timestamp = $timestamp, metaData = $metaData)"
  }
}