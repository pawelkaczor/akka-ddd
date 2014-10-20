package pl.newicom.dddd.eventhandling

import akka.actor.Actor
import pl.newicom.dddd.messaging.event.DomainEventMessage

trait LocalPublisher extends EventPublisher {
  this: Actor =>

  override def publish(em: DomainEventMessage) {
    context.system.eventStream.publish(em.event)
  }

}
