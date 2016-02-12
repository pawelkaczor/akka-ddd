package pl.newicom.eventstore

import akka.actor.{ActorSystem, Actor}
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Source
import eventstore.{ResolvedEvent, EventRecord, EsConnection}
import eventstore.EventNumber.Exact
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.EventMessageEntry
import akka.NotUsed

trait EventSourceProvider extends EventstoreSerializationSupport with pl.newicom.dddd.messaging.event.EventSourceProvider[EsConnection] {
  this: Actor =>

  override def system: ActorSystem = context.system

  def log: LoggingAdapter

  override def eventSource(eventStore: EsConnection, observable: BusinessEntity, fromPosExcl: Option[Long]): Source[EventMessageEntry, NotUsed] = {
    val streamId = StreamIdResolver.streamId(observable)
    log.debug(s"Subscribing to $streamId from position $fromPosExcl (exclusive)")
    Source.fromPublisher(
      eventStore.streamPublisher(
        streamId,
        fromPosExcl.map(nr => Exact(nr.toInt)),
        resolveLinkTos = true
      )
    ).map {
      case EventRecord(_, number, eventData, _) =>
        EventMessageEntry(toOfficeEventMessage(eventData).get, number.value)
      case ResolvedEvent(EventRecord(_, _, eventData, _), linkEvent) =>
        EventMessageEntry(toOfficeEventMessage(eventData).get, linkEvent.number.value)
      case unexpected =>
        throw new RuntimeException(s"Unexpected msg received: $unexpected")
    }
  }

}
