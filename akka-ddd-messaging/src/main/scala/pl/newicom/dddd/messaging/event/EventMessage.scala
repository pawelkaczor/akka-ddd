package pl.newicom.dddd.messaging.event

import pl.newicom.dddd.aggregate.DomainEvent
import pl.newicom.dddd.messaging.MetaAttribute.Correlation_Id
import pl.newicom.dddd.messaging.{AddressableMessage, Message, MetaData}

object EventMessage {
  def unapply(em: EventMessage): Option[(String, DomainEvent)] =
    Some(em.id, em.event)

  def apply(event: DomainEvent): EventMessage =
    apply(event, MetaData.initial())

  def apply(event0: DomainEvent, metaData0: MetaData): EventMessage = new EventMessage {

    override def event: DomainEvent = event0

    override type MessageImpl = EventMessage

    override def metadata: MetaData = metaData0

    override protected def withNewMetaData(newMetaData: MetaData): MessageImpl =
      EventMessage(event, newMetaData)
  }
}

trait EventMessage extends Message with AddressableMessage {

  type MessageImpl <: EventMessage

  def event: DomainEvent

  override def destination: Option[String] =
    tryGetMetaAttribute[String](Correlation_Id)

  override def payload: DomainEvent =
    event

  override def toString: String =
    s"EventMessage(event = $event, metaData = $metadata)"
}