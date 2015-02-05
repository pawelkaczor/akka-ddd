package pl.newicom.dddd.messaging.event

import java.util.UUID

import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.DomainEvent
import pl.newicom.dddd.messaging.event.EventMessage.CorrelationId
import pl.newicom.dddd.messaging.{EntityMessage, Message}

object EventMessage {
  val CorrelationId: String = "correlationId"

  def unapply(em: EventMessage): Option[(String, DomainEvent)] = {
    Some(em.identifier, em.event)
  }
}

class EventMessage(
    val event: DomainEvent,
    val identifier: String = UUID.randomUUID().toString,
    val timestamp: DateTime = new DateTime)
  extends Message with EntityMessage {

  override def entityId = tryGetMetaAttribute[String](CorrelationId).orNull
  override def payload = event

  override def toString: String = {
    val msgClass = getClass.getSimpleName
    s"$msgClass(event = $event, identifier = $identifier, timestamp = $timestamp, metaData = $metadata)"
  }
}