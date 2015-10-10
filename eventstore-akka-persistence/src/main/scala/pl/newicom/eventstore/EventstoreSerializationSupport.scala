package pl.newicom.eventstore

import akka.actor.ActorSystem
import akka.persistence.PersistentRepr
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent
import akka.util.ByteString
import com.typesafe.config.ConfigException
import eventstore.Content._
import eventstore.{Content, ContentType, EventData}
import org.joda.time.DateTime
import org.json4s.{NoTypeHints, FullTypeHints, TypeHints, Serializer}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.{AggregateSnapshotId, DomainEventMessage, EventMessage}
import pl.newicom.dddd.serialization.{NoSerializationHints, JsonSerializationHints}
import pl.newicom.eventstore.json.JsonSerializerExtension
import scala.util.{Failure, Success, Try}

/**
 * Contains methods for converting akka.persistence.PersistentRepr from/to eventstore.EventData.
 * Payload of deserialized (in-memory) PersistentRepr is EventMessage.
 * During serialization of PersistentRepr, its payload is replaced with payload of EventMessage (actual event) while
 * metadata of EventMessage is stored in metadata of eventstore.EventData.
 * EventType of eventstore.EventData is set to class of actual event.
 * </br>
 * During deserialization original (in-memory) PersistentRepr is reconstructed.
 */
trait EventstoreSerializationSupport {

  lazy val jsonSerializer = JsonSerializerExtension(system)

  def system: ActorSystem

  def hintsClassName = system.settings.config.getString("serialization.json.hints.class")

  def serializationHints: JsonSerializationHints = {
    Try(hintsClassName).flatMap {
      hintsClass => Try {
        Class.forName(hintsClass).getConstructor().newInstance().asInstanceOf[JsonSerializationHints]
      }
    }.recover {
      case ex: ConfigException.Missing =>
        NoSerializationHints
    }.get
  }

  def toEventData(x: AnyRef, contentType: ContentType): EventData = {
    def toContent(o: AnyRef, eventType: Option[String] = None) =
      Content(ByteString(serialize(o, eventType.toList)), contentType)

    x match {
      case x: PersistentRepr =>
        x.payload match {
          case em: EventMessage =>
            val (event, mdOpt) = toPayloadAndMetadata(em)
            val eventType = classFor(event).getName
            EventData(
              eventType = eventType,
              data = toContent(x.withPayload(event), Some(eventType)),
              metadata = mdOpt.fold(Empty)(md => toContent(md))
            )
          case _ =>
            EventData(eventType = classFor(x).getName, data = toContent(x))
        }

      case x: SnapshotEvent =>
        EventData(eventType = classFor(x).getName, data = toContent(x))

      case _ => sys.error(s"Cannot serialize $x")
    }
  }

  def fromEvent[A](event: EventData, manifest: Class[A]): Try[A] = {
    val result = deserialize(event.data.value.toArray, manifest, List(event.eventType))
    if (manifest.isInstance(result)) {
      Success((result match {
        case pr: PersistentRepr =>
          val mdOpt = event.metadata.value match {
            case bs: ByteString if bs.isEmpty => None
            case bs: ByteString               => Some(deserialize(bs.toArray, classOf[MetaData]))
          }
          pr.withPayload(fromPayloadAndMetadata(pr.payload.asInstanceOf[AnyRef], mdOpt))
        case _ => result
      }).asInstanceOf[A])
    } else
      Failure(sys.error(s"Cannot deserialize event as $manifest, event: $event"))
  }

  def toDomainEventMessage(eventData: EventData): Try[DomainEventMessage] =
    fromEvent(eventData, classOf[PersistentRepr]).map { pr =>
      val em = pr.payload.asInstanceOf[EventMessage]
      val aggrSnapId = new AggregateSnapshotId(pr.persistenceId, pr.sequenceNr)
      new DomainEventMessage(em, aggrSnapId).withMetaData(em.metadata).asInstanceOf[DomainEventMessage]
    }

  def toEventMessage(eventData: EventData): Try[EventMessage] = {
    fromEvent(eventData, classOf[PersistentRepr]).map {
      pr => pr.payload.asInstanceOf[EventMessage]
    }
  }

  private def toPayloadAndMetadata(em: EventMessage): (DomainEvent, Option[MetaData]) =
    (em.event, em.withMetaData(Map("id" -> em.id, "timestamp" -> em.timestamp)).metadata)

  private def fromPayloadAndMetadata(payload: AnyRef, maybeMetadata: Option[MetaData]): EventMessage = {
    if (maybeMetadata.isDefined) {
      val metadata = maybeMetadata.get
      val id: EntityId = metadata.get("id")
      val timestamp = DateTime.parse(metadata.get("timestamp"))
      new EventMessage(payload, id, timestamp).withMetaData(Some(metadata))
    } else {
      new EventMessage(payload)
    }
  }

  private def deserialize[T](bytes: Array[Byte], clazz: Class[T], hints: List[String] = List()): T =
    jsonSerializer.fromBinary(bytes, clazz, serializationHints ++ toSerializationHints(hints))

  private def serialize(o : AnyRef, hints: List[String] = List()): Array[Byte] =
    jsonSerializer.toBinary(o, serializationHints ++ toSerializationHints(hints))

  private def classFor(x: AnyRef) = x match {
    case x: PersistentRepr => classOf[PersistentRepr]
    case _                 => x.getClass
  }

  private def toSerializationHints(hints: List[String]) = new JsonSerializationHints {
    override def typeHints: TypeHints = if (hints.isEmpty) NoTypeHints else FullTypeHints(hints.map(Class.forName))
    override def serializers: List[Serializer[_]] = List()
  }

}
