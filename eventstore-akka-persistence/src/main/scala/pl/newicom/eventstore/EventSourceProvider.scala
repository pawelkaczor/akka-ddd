package pl.newicom.eventstore

import akka.actor.{Actor, ActorSystem}
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Source
import eventstore.EventNumber.Exact
import eventstore.{EsConnection, EventRecord, ResolvedEvent}
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.EventMessageEntry

trait EventSourceProvider extends EventstoreSerializationSupport
  with pl.newicom.dddd.messaging.event.EventSourceProvider with EventStoreProvider {
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
        val eventNumber = number.value
        EventMessageEntry(toOfficeEventMessage(eventData, eventNumber, observable).get, eventNumber, created)
      case ResolvedEvent(EventRecord(_, _, eventData, created), linkEvent) =>
        val eventNumber = linkEvent.number.value
        EventMessageEntry(toOfficeEventMessage(eventData, eventNumber, observable).get, eventNumber, created)
      case unexpected =>
        throw new RuntimeException(s"Unexpected msg received: $unexpected")
    }
  }

}
