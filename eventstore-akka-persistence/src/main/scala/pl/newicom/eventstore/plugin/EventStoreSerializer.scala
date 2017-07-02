package pl.newicom.eventstore.plugin

import java.nio.ByteBuffer
import java.nio.charset.Charset

import akka.actor.ExtendedActorSystem
import eventstore._
import pl.newicom.eventstore.EventstoreSerializationSupport

class EventStoreSerializer(override val system: ExtendedActorSystem)
  extends akka.persistence.eventstore.EventStoreSerializer with EventstoreSerializationSupport {

  def identifier = EventStoreSerializer.Identifier

  def includeManifest = true

  val errorMsg = "This serializer should be registered only for PersistentRepr and should be used only by EventStore journal"

  override def fromBinary(bytes: Array[Byte], manifestOpt: Option[Class[_]]): AnyRef =
    throw new UnsupportedOperationException(errorMsg)


  override def toBinary(o: AnyRef): Array[Byte] =
    throw new UnsupportedOperationException(errorMsg)

  override def fromEvent(event: Event, manifest: Class[_]): AnyRef =
    fromEvent(event.data, manifest.asInstanceOf[Class[AnyRef]]).get

  override def toEvent(o: AnyRef): EventData =
    toEventData(o, ContentType.Json)

}


object EventStoreSerializer {
  val UTF8: Charset = Charset.forName("UTF-8")
  val Identifier: Int = ByteBuffer.wrap("eventstore".getBytes(UTF8)).getInt
}