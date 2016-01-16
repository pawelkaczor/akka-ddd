package pl.newicom.eventstore

import akka.actor.ActorSystem
import akka.persistence.PersistentRepr
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent
import akka.util.ByteString
import eventstore.Content._
import eventstore.{Content, ContentType, EventData}
import org.joda.time.DateTime
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.{CaseId, EventMessage, OfficeEventMessage}
import pl.newicom.dddd.serialization.JsonSerHints._
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
  lazy val serializationHints = fromConfig(system.settings.config)

  def system: ActorSystem

  def toEventData(x: AnyRef, contentType: ContentType): EventData = {
    def toContent(o: AnyRef, eventType: Option[String] = None) =
      Content(ByteString(serialize(o, eventType)), contentType)

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
    val result = deserialize(event.data.value.toArray, manifest, Some(event.eventType))
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

  def toOfficeEventMessage(eventData: EventData): Try[OfficeEventMessage] =
    fromEvent(eventData, classOf[PersistentRepr]).map { pr =>
      val em = pr.payload.asInstanceOf[EventMessage]
      val caseId = new CaseId(pr.persistenceId, pr.sequenceNr)
      OfficeEventMessage(em, caseId)
    }

  private def toPayloadAndMetadata(em: EventMessage): (DomainEvent, Option[MetaData]) =
    (em.event, em.withMetaData(Map("id" -> em.id, "timestamp" -> em.timestamp)).metadata)

  private def fromPayloadAndMetadata(payload: AnyRef, maybeMetadata: Option[MetaData]): EventMessage = {
    if (maybeMetadata.isDefined) {
      val metadata = maybeMetadata.get
      val id: EntityId = metadata.get("id")
      val timestamp = DateTime.parse(metadata.get("timestamp"))
      EventMessage(payload, id, timestamp).withMetaData(Some(metadata))
    } else {
      EventMessage(payload)
    }
  }

  private def deserialize[T](bytes: Array[Byte], clazz: Class[T], eventType: Option[String] = None): T = {
    jsonSerializer.fromBinary(bytes, clazz, serializationHints ++ eventType.toList)
  }

  private def serialize(o : AnyRef, eventType: Option[String] = None): Array[Byte] =
    jsonSerializer.toBinary(o, serializationHints ++ eventType.toList)

  private def classFor(x: AnyRef) = x match {
    case x: PersistentRepr => classOf[PersistentRepr]
    case _                 => x.getClass
  }

}
