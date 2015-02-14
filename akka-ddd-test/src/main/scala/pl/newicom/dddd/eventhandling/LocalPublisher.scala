package pl.newicom.dddd.eventhandling

import akka.actor.Actor
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.event.DomainEventMessage

import scala.util.Success

trait LocalPublisher extends EventPublisher {
  this: Actor =>

  override abstract def handle(event: DomainEventMessage): Unit = {
    publish(event)
    sender ! Processed(Success(event.payload))
  }

  override def publish(em: DomainEventMessage) {
    context.system.eventStream.publish(em.event)
  }


}
