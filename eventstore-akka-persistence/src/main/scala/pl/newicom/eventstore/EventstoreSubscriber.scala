package pl.newicom.eventstore

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef}
import eventstore.EventNumber._
import eventstore.EventStream._
import eventstore._
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}

import scala.util.Success


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
