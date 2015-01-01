package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import eventstore.EsError.NotAuthenticated
import eventstore.EventNumber.Exact
import eventstore.EventStream.System
import eventstore._
import pl.newicom.eventstore.EventstoreSerializationSupport

import scala.util.Success

object ViewUpdater {
  def props(esConn: ActorRef, stream: String, viewHandler: ViewHandler): Props =
    Props(new ViewUpdater(esConn, stream, viewHandler))
}

class ViewUpdater(esConn: ActorRef, val stream: String, val viewHandler: ViewHandler)
  extends Actor with EventstoreSerializationSupport with ActorLogging {

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.actorOf(streamSubscription(stream, Exact(lastEventNumber)), s"$stream-subscription")
  }

  def streamSubscription(stream: String, lastEventNr: EventNumber) =
    StreamSubscriptionActor.props(esConn, self, System(s"ce-$stream"), Some(lastEventNr), resolveLinkTos = true)

  override def receive: Receive = {
    case Failure(EsException(NotAuthenticated, _)) =>
      log.error("Invalid credentials")
      throw new RuntimeException("Invalid credentials")

    case ResolvedEvent(EventRecord(streamId, Exact(eventNumber), eventData, _), _) =>
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


  private def isConsumed(eventNumber: Int) = eventNumber <= lastEventNumber

  private def lastEventNumber = viewHandler.lastEventNumber
}
