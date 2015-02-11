package pl.newicom.eventstore

import akka.actor.{ActorRef, ActorLogging}
import akka.actor.Status.Failure
import akka.persistence.PersistentActor
import eventstore.EventNumber._
import eventstore.EventStream._
import eventstore._
import org.json4s.ext.{JodaTimeSerializers, UUIDSerializer}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, Formats}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{DurableEventstreamSubscriber, EventMessage}

import scala.collection.immutable.Map

trait EventMessageUnmarshaller {

  val defaultFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all + UUIDSerializer
  implicit val formats: Formats = defaultFormats

  protected def unmarshallEventMessage(er: EventRecord): EventMessage = {
    val eventData = er.data
    val event = read[DomainEvent](eventData.data.value.utf8String)
    val metadata = read[Map[String, Any]](eventData.metadata.value.utf8String) ++ Map(
      EventPosition -> er.number.value.asInstanceOf[Long]
    )
    new EventMessage(event).withMetaData[EventMessage](metadata)
  }
}

trait EventstoreDurableSubscriber extends DurableEventstreamSubscriber with PersistentActor with EventMessageUnmarshaller with ActorLogging {

  def subscribe: ActorRef = {
    val position = nextSubscribePosition
    log.debug(s"Subscribing to $streamName from position $position (exclusive)")

    context.actorOf(
      StreamSubscriptionActor.props(
        EventStoreExtension(context.system).actor,
        self,
        Plain(streamName),
        position.map(l => Exact(l.toInt)),
        resolveLinkTos = false),
      s"subscription-$streamName")
  }

  def receiveEvent: Receive = {
    case er: EventRecord =>
      val em = unmarshallEventMessage(er)
      log.debug(s"Event received: $em")
      persist(em.withMetaData[EventMessage](customMetadata(em)))(updateState)

    case Failure(NotAuthenticated) =>
      log.error("Invalid credentials")
      throw new RuntimeException("Invalid credentials")

    case LiveProcessingStarted =>
      log.debug("Live processing started")

  }

}
