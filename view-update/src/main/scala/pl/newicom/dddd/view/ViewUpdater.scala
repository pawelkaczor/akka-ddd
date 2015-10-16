package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import eventstore.EventNumber.Exact
import eventstore._
import pl.newicom.dddd.messaging.event.OfficeEventStream
import pl.newicom.dddd.office.OfficeInfo
import pl.newicom.dddd.view.ViewUpdater.LastEventNr
import pl.newicom.eventstore.EventstoreSerializationSupport
import pl.newicom.eventstore.StreamNameResolver.streamId
import akka.pattern.pipe
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

object ViewUpdater {
  def props(esConn: ActorRef, officeInfo: OfficeInfo[_], viewHandler: ViewHandler): Props =
    Props(new ViewUpdater(esConn, streamId(OfficeEventStream(officeInfo)), viewHandler))

  case class LastEventNr(opt: Option[Exact])
}

class ViewUpdater(esConn: ActorRef, val streamId: EventStream.Id, val viewHandler: ViewHandler)
  extends Actor with EventstoreSerializationSupport with ActorLogging {

  override def system = context.system

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    lastEventNumber.map(ol => LastEventNr(ol.map(l => Exact(l.toInt)))).pipeTo(self)
  }

  def streamSubscription(streamId: EventStream.Id, lastEventNr: Option[EventNumber]) =
    StreamSubscriptionActor.props(esConn, self, streamId, lastEventNr, resolveLinkTos = true)

  override def receive: Receive = {
    case Failure(NotAuthenticated) =>
      log.error("Invalid credentials")
      throw new RuntimeException("Invalid credentials")

    case Failure(cause) =>
      throw cause

    case LastEventNr(lastEvNum) =>
      context.actorOf(streamSubscription(streamId, lastEvNum), s"${streamId.value}-subscription")
      log.debug(s"Subscribed to $streamId, lastEventNumber = $lastEvNum")

    case ResolvedEvent(EventRecord(_, _, eventData, _), linkEvent) =>
      val eventNumber = linkEvent.number.value
      toDomainEventMessage(eventData) match {
        case Success(em) =>
          isConsumed(eventNumber).collect {
            case false => viewHandler.handle(em, eventNumber)
          }
        case scala.util.Failure(cause) =>
          throw cause
      }
    case e =>
      log.debug(s"RECEIVED: $e")
  }


  private def isConsumed(eventNumber: Long) =
    lastEventNumber.map(eventNumber <= _.getOrElse(-1L))

  private def lastEventNumber = viewHandler.lastEventNumber
}
