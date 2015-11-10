package pl.newicom.dddd.messaging.event

import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.DomainEvent
import pl.newicom.dddd.messaging.MetaData.CorrelationId
import pl.newicom.dddd.messaging.{EntityMessage, Message}
import pl.newicom.dddd.utils.UUIDSupport._

object EventMessage {
  def unapply(em: EventMessage): Option[(String, DomainEvent)] = {
    Some(em.id, em.event)
  }
}

class EventMessage(
    val event: DomainEvent,
    val id: String = uuid,
    val timestamp: DateTime = new DateTime)
  extends Message with EntityMessage {

  type MessageImpl <: EventMessage

  override def entityId = tryGetMetaAttribute[String](CorrelationId).orNull
  override def payload = event

  override def toString: String = {
    val msgClass = getClass.getSimpleName
    s"$msgClass(event = $event, id = $id, timestamp = $timestamp, metaData = $metadata)"
  }
}