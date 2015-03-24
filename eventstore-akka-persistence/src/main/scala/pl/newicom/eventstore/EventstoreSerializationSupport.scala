package pl.newicom.eventstore

import akka.actor.Actor
import akka.persistence.PersistentRepr
import akka.serialization.{Serialization, SerializationExtension}
import eventstore.EventData
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.{EventMessage, AggregateSnapshotId, DomainEventMessage}

import scala.reflect.ClassTag
import scala.util.Try

trait EventstoreSerializationSupport {
  this: Actor =>

  val serialization: Serialization = SerializationExtension(context.system)

  def toDomainEventMessage(eventData: EventData): Try[DomainEventMessage] = {
    for {
        pr <- deserialize[PersistentRepr](eventData.data.value.toArray)
        metadata <- deserialize[MetaData](eventData.metadata.value.toArray)
    } yield {
      toDomainEventMessage(pr, metadata)
    }
  }

  def toEventMessage(eventData: EventData): Try[EventMessage] = {
    for {
      pr <- deserialize[PersistentRepr](eventData.data.value.toArray)
      metadata <- deserialize[MetaData](eventData.metadata.value.toArray)
    } yield {
      toEventMessage(pr, metadata)
    }
  }

  private def deserialize[A](bytes: Array[Byte])(implicit ct: ClassTag[A]): Try[A] =
    serialization.deserialize(bytes, ct.runtimeClass).asInstanceOf[Try[A]]

  private def toDomainEventMessage(pr: PersistentRepr, metadata: MetaData) = {
    val id: EntityId = metadata.get("id")
    val aggrSnapId = new AggregateSnapshotId(pr.persistenceId, pr.sequenceNr)
    val event: AnyRef = pr.payload.asInstanceOf[AnyRef]
    new DomainEventMessage(aggrSnapId, event, id).withMetaData(Some(metadata)).asInstanceOf[DomainEventMessage]
  }

  private def toEventMessage(pr: PersistentRepr, metadata: MetaData) = {
    val id: EntityId = metadata.get("id")
    val event: AnyRef = pr.payload.asInstanceOf[AnyRef]
    new EventMessage(event, id).withMetaData(Some(metadata))
  }

}
