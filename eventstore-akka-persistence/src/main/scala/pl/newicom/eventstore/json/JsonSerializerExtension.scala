package pl.newicom.eventstore.json

import java.nio.charset.Charset

import akka.actor._
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent.Snapshot
import akka.persistence.{PersistentRepr, SnapshotMetadata}
import akka.serialization.{Serialization, SerializationExtension, SerializerWithStringManifest}
import org.json4s.Extraction.decompose
import org.json4s.JsonAST._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.{read, write}
import org.json4s.{CustomSerializer, Formats, FullTypeHints, JValue, Serializer, TypeInfo}
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.delivery.protocol.alod.{Processed => AlodProcessed}
import pl.newicom.dddd.messaging.{MetaData, PublisherTypeValue}
import pl.newicom.dddd.scheduling.{EventScheduled, ScheduledEventMetadata}
import pl.newicom.dddd.serialization.{JsonExtraSerHints, JsonSerHints}
import pl.newicom.dddd.serialization.JsonSerHints._

/**
 * The reason for using Extension mechanism is that
 * pl.newicom.eventstore.json.JsonSerializerExtensionImpl.ActorRefSerializer
 * requires access to ExtendedActorSystem
 *
 * @param system ExtendedActorSystem (injected automatically by Akka)
 */
class JsonSerializerExtensionImpl(system: ExtendedActorSystem) extends Extension {

  val extraHints = JsonExtraSerHints(
    typeHints =
      FullTypeHints(
        List(classOf[MetaData], classOf[Processed], classOf[AlodProcessed], classOf[PersistentRepr], classOf[EventScheduled])),
    serializers =
      List(ActorRefSerializer, ActorPathSerializer, new ScheduledEventSerializer, SnapshotJsonSerializer(system), new EnumNameSerializer(PublisherTypeValue))
  )

  val UTF8: Charset = Charset.forName("UTF-8")

  def fromBinary[A](bytes: Array[Byte], clazz: Class[A], hints: JsonSerHints): A = {
    implicit val formats: Formats = hints ++ extraHints
    implicit val manifest: Manifest[A] = Manifest.classType(clazz)
    try {
      read(new String(bytes, UTF8))
    } catch {
      case th: Throwable =>
        th.printStackTrace()
        throw th;
    }
  }

  def toBinary(o: AnyRef, hints: JsonSerHints): Array[Byte] = {
    implicit val formats: Formats = hints ++ extraHints
    write(o).getBytes(UTF8)
  }

  object ActorRefSerializer extends CustomSerializer[ActorRef](_ => (
    {
      case JString(s) => system.provider.resolveActorRef(s)
      case JNull => null
    },
    {
      case x: ActorRef => JString(Serialization.serializedActorPath(x))
    }
    ))

}

object JsonSerializerExtension extends ExtensionId[JsonSerializerExtensionImpl] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem) = new JsonSerializerExtensionImpl(system)
  override def lookup(): ExtensionId[_ <: Extension] = JsonSerializerExtension
  override def get(system: ActorSystem): JsonSerializerExtensionImpl = super.get(system)

}

object ActorPathSerializer extends CustomSerializer[ActorPath](_ => (
  { case JString(s) => ActorPath.fromString(s) },
  { case x: ActorPath => JString(x.toSerializationFormat) }
  ))

class ScheduledEventSerializer extends Serializer[EventScheduled] {
  val Clazz: Class[EventScheduled] = classOf[EventScheduled]

  def deserialize(implicit formats: Formats): PartialFunction[(TypeInfo, JValue), EventScheduled] = {
    case (TypeInfo(Clazz, _), JObject(List(
            JField("metadata", metadata),
            JField("eventClass", JString(eventClassName)),
            JField("event", event)))) =>
              val eventClass = Class.forName(eventClassName)
              val eventObj = event.extract[AnyRef](formats, Manifest.classType(eventClass))
              val metadataObj = metadata.extract[ScheduledEventMetadata]
              EventScheduled(metadataObj, eventObj)
  }

  def serialize(implicit formats: Formats): PartialFunction[Any, JObject] = {
    case EventScheduled(metadata, event) =>
      JObject(
        "jsonClass"   -> JString(classOf[EventScheduled].getName),
        "metadata"    -> decompose(metadata),
        "eventClass"  -> JString(event.getClass.getName),
        "event"       -> decompose(event)
      )
  }
}

case class SnapshotDataSerializationResult(data: String, serializerId: Option[Int], manifest: String)

case class SnapshotJsonSerializer(sys: ActorSystem) extends Serializer[SnapshotEvent] {
  val Clazz: Class[SnapshotEvent] = classOf[SnapshotEvent]
  val EmptySerializerId: Int = 0

  import akka.serialization.{Serialization => SysSerialization}
  lazy val serialization: SysSerialization = SerializationExtension(sys)

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), SnapshotEvent] = {
    case (TypeInfo(Clazz, _), JObject(List(
            JField("dataClass", JString(dataClass)),
            JField("dataSerializerId", JInt(serializerId)),
            JField("data", JString(x)),
            JField("metadata", metadata)))) =>
              import Base64._

              val data = if (serializerId.intValue == EmptySerializerId) {
                serialization.deserialize(x.toByteArray, Class.forName(dataClass)).get
              } else {
                serialization.deserialize(x.toByteArray, serializerId.intValue, dataClass).get
              }
              val metaData = metadata.extract[SnapshotMetadata]
              Snapshot(data, metaData)
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JObject] = {

    case Snapshot(data, metadata) =>
      val serResult = data match {
        case data: AnyRef =>
          val serializer = serialization.serializerFor(data.getClass)
          val manifest:String = serializer match {
            case s:SerializerWithStringManifest => s.manifest(data.asInstanceOf[AnyRef])
            case _                              => data.getClass.getName
          }
          import Base64._
          SnapshotDataSerializationResult(serializer.toBinary(data).toBase64, Some(serializer.identifier), manifest)
        case _ =>
          SnapshotDataSerializationResult(data.toString, None, classOf[String].getName)
      }

      JObject(
        "jsonClass" -> JString(Clazz.getName),
        "dataClass" -> JString(serResult.manifest),
        "dataSerializerId" -> JInt(serResult.serializerId.getOrElse[Int](EmptySerializerId)),
        "data"      -> JString(serResult.data),
        "metadata"  -> decompose(metadata)
      )
  }
}
