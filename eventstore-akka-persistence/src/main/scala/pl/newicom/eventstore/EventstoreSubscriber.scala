package pl.newicom.eventstore

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef}
import eventstore.EventNumber._
import eventstore._
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}
import scala.util.Success

trait EventstoreSubscriber extends EventStreamSubscriber with EventstoreSerializationSupport with ActorLogging {
  this: Actor =>

  override def system = context.system

  def subscribe(stream: pl.newicom.dddd.messaging.event.EventStream, fromPositionExclusive: Option[Long]): ActorRef = {
    val streamId = StreamNameResolver.streamId(stream)
    log.debug(s"Subscribing to $streamId from position $fromPositionExclusive (exclusive)")

    context.actorOf(
      StreamSubscriptionActor.props(
        EventStoreExtension(context.system).actor,
        self,
        streamId,
        fromPositionExclusive.map(l => Exact(l.toInt)),
        resolveLinkTos = true),
      s"subscription-${streamId.value}")
  }

  def eventReceived(eventData: EventData, eventNumber: Int, metaDataProvider: EventMessage => Option[MetaData]): Unit = {
    toEventMessage(eventData) match {
      case Success(em) =>
        eventReceived(em.withMetaData(metaDataProvider(em)), eventNumber)
      case scala.util.Failure(cause) =>
        throw cause
    }
  }

  def receiveEvent(metaDataProvider: EventMessage => Option[MetaData]): Receive = {
    case EventRecord(streamId, number, eventData, _) =>
      eventReceived(eventData, number.value, metaDataProvider)
      
    case ResolvedEvent(EventRecord(streamId, _, eventData, _), linkEvent) =>
      eventReceived(eventData, linkEvent.number.value, metaDataProvider)

    case Failure(NotAuthenticated) =>
      log.error("Invalid credentials")
      throw new RuntimeException("Invalid credentials")

    case LiveProcessingStarted =>
      log.debug("Live processing started")
  }

}
