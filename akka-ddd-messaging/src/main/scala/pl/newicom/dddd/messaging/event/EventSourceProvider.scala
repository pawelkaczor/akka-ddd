package pl.newicom.dddd.messaging.event

import akka.NotUsed
import akka.stream.scaladsl.Source
import pl.newicom.dddd.aggregate.BusinessEntity

trait EventSourceProvider extends EventStoreProvider {
  type EventSource = Source[EventMessageEntry, NotUsed]

  def eventSource(eventStore: EventStore, observable: BusinessEntity, fromPosExcl: Option[Long]): EventSource

}
