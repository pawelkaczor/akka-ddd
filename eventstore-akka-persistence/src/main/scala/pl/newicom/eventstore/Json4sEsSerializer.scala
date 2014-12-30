package pl.newicom.eventstore

import java.nio.ByteBuffer
import java.nio.charset.Charset

import akka.persistence.SnapshotMetadata
import akka.persistence.eventstore.EventStoreSerializer
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent.Snapshot
import akka.util.ByteString
import eventstore.{Content, ContentType, Event, EventData}
import org.joda.time.DateTime
import org.json4s.Extraction.decompose
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s._
import org.json4s.ext.{JodaTimeSerializers, UUIDSerializer}
import org.json4s.native.Serialization.{read, write}
import org.json4s.reflect.TypeInfo
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.eventstore.Json4sEsSerializer._

class Json4sEsSerializer extends EventStoreSerializer {

  def identifier = Identifier

  val defaultFormats: Formats = DefaultFormats +
    SnapshotSerializer ++ JodaTimeSerializers.all + UUIDSerializer + new ShortTypeHints(List(classOf[MetaData]))

  implicit val formats: Formats = defaultFormats

  def includeManifest = true

  override def fromBinary(bytes: Array[Byte], manifestOpt: Option[Class[_]]) = {
    implicit val manifest = manifestOpt match {
      case Some(x) => Manifest.classType[AnyRef](x)
      case None    => Manifest.AnyRef
    }
    read(new String(bytes, UTF8))
  }

  override def toBinary(o: AnyRef) = {
    write(o).getBytes(UTF8)
  }


  override def toPayloadAndMetadata(e: AnyRef) = e match {
    case em: EventMessage => (em.event,
      em.withMetaData[EventMessage](Map("id" -> em.identifier, "timestamp" -> em.timestamp)).metadata)
    case _ => super.toPayloadAndMetadata(e)
  }

  override def fromPayloadAndMetadata(payload: AnyRef, maybeMetadata: Option[AnyRef]): AnyRef = {
    if (maybeMetadata.isDefined) {
      val metadata = maybeMetadata.get.asInstanceOf[MetaData]
      val id: EntityId = metadata.get("id")
      val timestamp = DateTime.parse(metadata.get("timestamp"))
      new EventMessage(payload, id, timestamp).withMetaData[EventMessage](Some(metadata))
    } else {
      new EventMessage(payload)
    }
  }

  override def toEvent(x: AnyRef) = x match {
    case x: SnapshotEvent => EventData(
      eventType = x.getClass.getName,
      data = Content(ByteString(toBinary(x)), ContentType.Json))

    case _ => sys.error(s"Cannot serialize $x, SnapshotEvent expected")
  }

  override def fromEvent(event: Event, manifest: Class[_]) = {
    val clazz = Class.forName(event.data.eventType)
    val result = fromBinary(event.data.data.value.toArray, clazz)
    if (manifest.isInstance(result)) result
    else sys.error(s"Cannot deserialize event as $manifest, event: $event")
  }
  def contentType = ContentType.Json
}

object Json4sEsSerializer {
  val UTF8 = Charset.forName("UTF-8")
  val Identifier: Int = ByteBuffer.wrap("json4s".getBytes(UTF8)).getInt

  object SnapshotSerializer extends Serializer[Snapshot] {
    val Clazz = classOf[Snapshot]

    def deserialize(implicit format: Formats) = {
      case (TypeInfo(Clazz, _), JObject(List(
      JField("data", JString(x)),
      JField("metadata", metadata)))) => Snapshot(x, metadata.extract[SnapshotMetadata])
    }

    def serialize(implicit format: Formats) = {
      case Snapshot(data, metadata) => JObject("data" -> JString(data.toString), "metadata" -> decompose(metadata))
    }
  }
}