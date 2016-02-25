package pl.newicom.eventstore

import akka.actor._
import pl.newicom.dddd.messaging.event.DefaultEventStreamSubscriber

trait EventstoreSubscriber extends DefaultEventStreamSubscriber with EventSourceProvider {
  this: Actor =>

}