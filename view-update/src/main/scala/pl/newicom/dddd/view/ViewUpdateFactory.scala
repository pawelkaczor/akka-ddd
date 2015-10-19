package pl.newicom.dddd.view

import akka.stream.scaladsl.{RunnableGraph, Sink, Source}
import eventstore.EventNumber.Exact
import eventstore._
import org.slf4j.LoggerFactory
import pl.newicom.dddd.messaging.event.OfficeEventStream
import pl.newicom.dddd.office.OfficeInfo
import pl.newicom.dddd.view.ViewUpdateFactory.{ViewUpdate, EventReceived}
import pl.newicom.eventstore.{EventstoreSerializationSupport, StreamNameResolver}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ViewUpdateFactory {

  object EventReceived {
    def apply(eventData: EventData, eventNr: Long, lastEventNrOpt: Option[Long]): EventReceived = 
      EventReceived(eventData, eventNr, eventNr <= lastEventNrOpt.getOrElse(-1L))
  }
  
  case class EventReceived(eventData: EventData, eventNr: Long, alreadyProcessed: Boolean)
  
  case class ViewUpdate(runnable: RunnableGraph[Unit])
}

trait ViewUpdateFactory {
  this: EventstoreSerializationSupport =>

  lazy val logger = LoggerFactory.getLogger(getClass.getName)

  def esConnection: EsConnection

  def runnableViewUpdate(officeInfo: OfficeInfo[_], viewHandler: ViewHandler): Future[ViewUpdate] = {
    viewHandler.lastEventNumber.map { lastEvtNrOpt =>
      val streamId = StreamNameResolver.streamId(OfficeEventStream(officeInfo))
      logger.debug(s"Subscribed to $streamId, lastEventNumber = $lastEvtNrOpt")

      ViewUpdate(
        eventSource(streamId, lastEvtNrOpt)
          .map {
            case ResolvedEvent(EventRecord(_, _, eventData, _), linkEvent) =>
              EventReceived(eventData, linkEvent.number.value, lastEvtNrOpt)
            case unexpected =>
              throw new RuntimeException(s"Unexpected msg received: $unexpected")
          }.filter {
            !_.alreadyProcessed
          }.mapAsync(1) {
            event => viewHandler.handle(toDomainEventMessage(event.eventData).get, event.eventNr)
          }.to {
            Sink.ignore
          }
      )
    }
  }

  private def eventSource(streamId: EventStream.Id, lastEvtNrOpt: Option[Long]): Source[Event, Unit] =
    Source(
      esConnection.streamPublisher(
        streamId, 
        lastEvtNrOpt.map(nr => Exact(nr.toInt)), 
        resolveLinkTos = true
      )
    )

}
