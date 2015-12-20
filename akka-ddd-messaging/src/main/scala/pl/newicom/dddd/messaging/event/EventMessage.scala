package pl.newicom.dddd.messaging.event

import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.DomainEvent
import pl.newicom.dddd.messaging.MetaData.CorrelationId
import pl.newicom.dddd.messaging.{MetaData, AddressableMessage, Message}
import pl.newicom.dddd.utils.UUIDSupport._

object EventMessage {
  def unapply(em: EventMessage): Option[(String, DomainEvent)] = {
    Some(em.id, em.event)
  }

  def apply(
             event0: DomainEvent,
             id0: String = uuid,
             timestamp0: DateTime = new DateTime,
             metaData0: Option[MetaData] = None): EventMessage = new EventMessage {

    override def event: DomainEvent = event0

    override def timestamp: DateTime = timestamp0

    override def id: String = id0

    override type MessageImpl = EventMessage

    override def metadata: Option[MetaData] = metaData0

    override def copyWithMetaData(newMetaData: Option[MetaData]): MessageImpl =
      EventMessage(event, id, timestamp, newMetaData)
  }
}

trait EventMessage extends Message with AddressableMessage {

  type MessageImpl <: EventMessage

  def event: DomainEvent
  def id: String
  def timestamp: DateTime

  override def destination = tryGetMetaAttribute[String](CorrelationId)
  override def payload = event

  override def toString: String = {
    s"EventMessage(event = $event, id = $id, timestamp = $timestamp, metaData = $metadata)"
  }
}