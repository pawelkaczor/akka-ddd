package pl.newicom.eventstore

import akka.actor._
import eventstore._
import pl.newicom.dddd.messaging.event.DefaultEventStreamSubscriber

trait EventstoreSubscriber extends DefaultEventStreamSubscriber[EsConnection] with EventSourceProvider {
  this: Actor =>

  override def eventStore: EsConnection = {
    EsConnection(system)
  }
}