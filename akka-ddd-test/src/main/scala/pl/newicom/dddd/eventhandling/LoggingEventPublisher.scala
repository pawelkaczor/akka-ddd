package pl.newicom.dddd.eventhandling

import akka.actor.Actor
import pl.newicom.dddd.messaging.event.OfficeEventMessage

trait LoggingEventPublisher extends EventPublisher {
  this: Actor =>

  override def publish(em: OfficeEventMessage) {
    println("Published: " + em)
  }

}
