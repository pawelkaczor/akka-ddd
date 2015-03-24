package pl.newicom.eventstore

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef}
import eventstore.EventNumber._
import eventstore.EventStream._
import eventstore._
import org.json4s.ext.{JodaTimeSerializers, UUIDSerializer}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, Formats, FullTypeHints}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.alod.Processed
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}

import scala.collection.immutable.Map
import scala.util.Success

trait EventMessageUnmarshaller {

  val defaultFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all + UUIDSerializer + FullTypeHints(List(classOf[Processed], classOf[Success[_]]))
  implicit val formats: Formats = defaultFormats

  def unmarshallEventMessage(er: EventRecord): (EventMessage, Long) = {
    val eventData = er.data
    val data = read[Map[String, Any]](eventData.data.value.utf8String)
    val event = data("payload").asInstanceOf[DomainEvent]
    val metadata = read[Map[String, Any]](eventData.metadata.value.utf8String)
    val position = er.number.value.asInstanceOf[Long]
    (new EventMessage(event).withMetaData(metadata), position)
  }
}

trait EventstoreSubscriber extends EventStreamSubscriber with EventstoreSerializationSupport with ActorLogging {
  this: Actor =>

  def subscribe(streamName: String, fromPositionExclusive: Option[Long]): ActorRef = {
    log.debug(s"Subscribing to $streamName from position $fromPositionExclusive (exclusive)")

    context.actorOf(
      StreamSubscriptionActor.props(
        EventStoreExtension(context.system).actor,
        self,
        Plain(streamName),
        fromPositionExclusive.map(l => Exact(l.toInt)),
        resolveLinkTos = true),
      s"subscription-$streamName")
  }

  def receiveEvent(metaDataProvider: EventMessage => Option[MetaData]): Receive = {
    case ResolvedEvent(EventRecord(streamId, _, eventData, _), linkEvent) =>
      val eventNumber = linkEvent.number.value
      toEventMessage(eventData) match {
        case Success(em) =>
          eventReceived(em.withMetaData(metaDataProvider(em)), eventNumber)
        case scala.util.Failure(cause) =>
          throw cause
      }

    case Failure(NotAuthenticated) =>
      log.error("Invalid credentials")
      throw new RuntimeException("Invalid credentials")

    case LiveProcessingStarted =>
      log.debug("Live processing started")
  }

}
