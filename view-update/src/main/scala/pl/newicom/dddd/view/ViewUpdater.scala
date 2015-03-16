package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import eventstore.EventNumber.Exact
import eventstore.EventStream.System
import eventstore._
import pl.newicom.dddd.office.OfficeInfo
import pl.newicom.eventstore.EventstoreSerializationSupport

import scala.util.Success

object ViewUpdater {
  def props(esConn: ActorRef, officeInfo: OfficeInfo[_], viewHandler: ViewHandler): Props =
    Props(new ViewUpdater(esConn, officeInfo.name, viewHandler))
}

class ViewUpdater(esConn: ActorRef, val stream: String, val viewHandler: ViewHandler)
  extends Actor with EventstoreSerializationSupport with ActorLogging {

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.actorOf(streamSubscription(stream, lastEventNumber.map(l => Exact(l.toInt))), s"$stream-subscription")
  }

  def streamSubscription(stream: String, lastEventNr: Option[EventNumber]) =
    StreamSubscriptionActor.props(esConn, self, System(s"ce-$stream"), lastEventNr, resolveLinkTos = true)

  override def receive: Receive = {
    case Failure(NotAuthenticated) =>
      log.error("Invalid credentials")
      throw new RuntimeException("Invalid credentials")

    case Failure(cause) =>
      log.error(s"Failure: $cause")
      throw cause

    case ResolvedEvent(EventRecord(streamId, _, eventData, _), linkEvent) =>
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
