package pl.newicom.eventstore.json

import java.nio.charset.Charset

import akka.actor._
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent.Snapshot
import akka.persistence.{PersistentRepr, SnapshotMetadata}
import akka.serialization.{Serialization, SerializationExtension}
import org.json4s.Extraction.decompose
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.ext.{JodaTimeSerializers, UUIDSerializer}
import org.json4s.native.Serialization.{read, write}
import org.json4s.reflect.TypeInfo
import org.json4s.{DefaultFormats, Formats, FullTypeHints, _}
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.delivery.protocol.alod.{Processed => AlodProcessed}
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.serialization.{NoSerializationHints, JsonSerializationHints}

/**
 * The reason for using Extension mechanism is that
 * pl.newicom.eventstore.json.JsonSerializerExtensionImpl.ActorRefSerializer
 * requires access to ExtendedActorSystem
 *
 * @param system ExtendedActorSystem (injected automatically by Akka)
 */
class JsonSerializerExtensionImpl(system: ExtendedActorSystem) extends Extension {

  val UTF8 = Charset.forName("UTF-8")

  val defaultFormats: Formats = DefaultFormats +
    ActorRefSerializer + new SnapshotJsonSerializer(system) ++ JodaTimeSerializers.all + UUIDSerializer +
    new FullTypeHints(List(
      classOf[MetaData],
      classOf[Processed],
      classOf[AlodProcessed],
      classOf[PersistentRepr]))

  def fromBinary[A](bytes: Array[Byte], clazz: Class[A], hints: JsonSerializationHints = NoSerializationHints): A = {
    implicit val formats: Formats = hints ++ defaultFormats
    implicit val manifest: Manifest[A] = Manifest.classType(clazz)
    try {
      read(new String(bytes, UTF8))
    } catch {
      case th: Throwable =>
        th.printStackTrace()
        throw th;
    }
  }

  def toBinary(o: AnyRef, hints: JsonSerializationHints = NoSerializationHints) = {
    implicit val formats: Formats = hints ++ defaultFormats
    write(o).getBytes(UTF8)
  }

  object ActorRefSerializer extends CustomSerializer[ActorRef](format => (
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
  override def get(system: ActorSystem) = super.get(system)
}

case class SnapshotJsonSerializer(sys: ActorSystem) extends Serializer[Snapshot] {
  val Clazz = classOf[Snapshot]

  import akka.serialization.{Serialization => SysSerialization}
  lazy val serialization: SysSerialization = SerializationExtension(sys)

  def deserialize(implicit format: Formats) = {
    case (TypeInfo(Clazz, _),
    JObject(List(
    JField("dataClass", JString(dataClass)),
    JField("data", JString(x)),
    JField("metadata", metadata)))) =>
      import Base64._
      val data = serialization.deserialize(x.toByteArray, Class.forName(dataClass)).get
      val metaData = metadata.extract[SnapshotMetadata]
      Snapshot(data, metaData)
  }

  def serializeAnyRef(data: AnyRef)(implicit format: Formats): String = {
    import Base64._
    serialization.serialize(data).get.toBase64
  }

  def serialize(implicit format: Formats) = {
    case Snapshot(data, metadata) =>
      val dataSerialized: String = data match {
        case data: AnyRef => serializeAnyRef(data)
        case _ => data.toString
      }
      JObject("jsonClass" -> JString(Clazz.getName), "dataClass" -> JString(data.getClass.getName), "data" -> JString(dataSerialized), "metadata" -> decompose(metadata))
  }
}
