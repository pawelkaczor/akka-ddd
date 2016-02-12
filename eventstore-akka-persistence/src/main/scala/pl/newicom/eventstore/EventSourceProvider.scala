package pl.newicom.eventstore

import akka.actor.{Actor, ActorSystem}
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Source
import eventstore.EventNumber.Exact
import eventstore.{EsConnection, EventRecord, ResolvedEvent}
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.EventMessageEntry

trait EventSourceProvider extends EventstoreSerializationSupport with pl.newicom.dddd.messaging.event.EventSourceProvider[EsConnection] {
  this: Actor =>

  override def system: ActorSystem = context.system

  def log: LoggingAdapter

  override def eventSource(eventStore: EsConnection, observable: BusinessEntity, fromPosExcl: Option[Long]): EventSource = {
    val streamId = StreamIdResolver.streamId(observable)
    log.debug(s"Subscribing to $streamId from position $fromPosExcl (exclusive)")
    Source.fromPublisher(
      eventStore.streamPublisher(
        streamId,
        fromPosExcl.map(nr => Exact(nr.toInt)),
        resolveLinkTos = true
      )
    ).map {
      case EventRecord(_, number, eventData, created) =>
        EventMessageEntry(toOfficeEventMessage(eventData).get, number.value, created)
      case ResolvedEvent(EventRecord(_, _, eventData, created), linkEvent) =>
        EventMessageEntry(toOfficeEventMessage(eventData).get, linkEvent.number.value, created)
      case unexpected =>
        throw new RuntimeException(s"Unexpected msg received: $unexpected")
    }
  }

}
