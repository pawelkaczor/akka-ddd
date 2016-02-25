package pl.newicom.dddd.messaging.event

import scala.concurrent.Future

trait EventStoreProvider {
  type EventStore

  def eventStore: EventStore

  def ensureEventStoreAvailable(): Future[EventStore]
}
