package pl.newicom.eventstore

import akka.event.LoggingAdapter
import akka.stream.scaladsl.Source
import eventstore.{ResolvedEvent, EventRecord, EsConnection}
import eventstore.EventNumber.Exact
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.EventMessageRecord

trait EventSourceProvider extends EventstoreSerializationSupport {

  def log: LoggingAdapter

  def eventSource(esCon: EsConnection, observable: BusinessEntity, fromPosExcl: Option[Long]): Source[EventMessageRecord, Unit] = {
    val streamId = StreamNameResolver.streamId(observable)
    log.debug(s"Subscribing to $streamId from position $fromPosExcl (exclusive)")
    Source(
      esCon.streamPublisher(
        streamId,
        fromPosExcl.map(nr => Exact(nr.toInt)),
        resolveLinkTos = true
      )
    ).map {
      case EventRecord(_, number, eventData, _) =>
        EventMessageRecord(toOfficeEventMessage(eventData).get, number.value)
      case ResolvedEvent(EventRecord(_, _, eventData, _), linkEvent) =>
        EventMessageRecord(toOfficeEventMessage(eventData).get, linkEvent.number.value)
      case unexpected =>
        throw new RuntimeException(s"Unexpected msg received: $unexpected")
    }
  }

}
