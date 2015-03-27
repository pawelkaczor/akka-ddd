package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import eventstore.EventNumber.Exact
import eventstore._
import pl.newicom.dddd.messaging.event.OfficeEventStream
import pl.newicom.dddd.office.OfficeInfo
import pl.newicom.eventstore.EventstoreSerializationSupport
import pl.newicom.eventstore.StreamNameResolver.streamId

import scala.util.Success

object ViewUpdater {
  def props(esConn: ActorRef, officeInfo: OfficeInfo[_], viewHandler: ViewHandler): Props =
    Props(new ViewUpdater(esConn, streamId(OfficeEventStream(officeInfo)), viewHandler))
}

class ViewUpdater(esConn: ActorRef, val streamId: EventStream.Id, val viewHandler: ViewHandler)
  extends Actor with EventstoreSerializationSupport with ActorLogging {

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    val lastEvNum: Option[Exact] = lastEventNumber.map(l => Exact(l.toInt))
    context.actorOf(streamSubscription(streamId, lastEvNum), s"${streamId.value}-subscription")
    log.debug(s"Subscribed to $streamId, lastEventNumber = $lastEvNum")
  }

  def streamSubscription(streamId: EventStream.Id, lastEventNr: Option[EventNumber]) =
    StreamSubscriptionActor.props(esConn, self, streamId, lastEventNr, resolveLinkTos = true)

  override def receive: Receive = {
    case Failure(NotAuthenticated) =>
      log.error("Invalid credentials")
      throw new RuntimeException("Invalid credentials")

    case Failure(cause) =>
      throw cause

    case ResolvedEvent(EventRecord(_, _, eventData, _), linkEvent) =>
      val eventNumber = linkEvent.number.value
      toDomainEventMessage(eventData) match {
        case Success(em) =>
          if (!isConsumed(eventNumber)) {
            viewHandler.handle(em, eventNumber)
          }
        case scala.util.Failure(cause) =>
          throw cause
      }
    case e =>
      log.debug(s"RECEIVED: $e")
  }


  private def isConsumed(eventNumber: Long) = eventNumber <= lastEventNumber.getOrElse(-1L)

  private def lastEventNumber: Option[Long] = viewHandler.lastEventNumber
}
