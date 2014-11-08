package pl.newicom.dddd.eventhandling

import akka.actor.Actor
import pl.newicom.dddd.messaging.event.DomainEventMessage

trait LoggingEventPublisher extends EventPublisher {
  this: Actor =>

  override def publish(em: DomainEventMessage) {
    println("Published: " + em)
  }

}
