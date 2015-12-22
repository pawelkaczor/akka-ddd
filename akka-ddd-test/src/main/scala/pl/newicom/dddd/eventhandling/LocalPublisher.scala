package pl.newicom.dddd.eventhandling

import akka.actor.{Actor, ActorRef}
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.event.OfficeEventMessage

import scala.util.Success

trait LocalPublisher extends EventPublisher {
  this: Actor =>

  override abstract def handle(senderRef: ActorRef, event: OfficeEventMessage): Unit = {
    publish(event)
    senderRef ! Processed(Success(event.payload))
  }

  override def publish(em: OfficeEventMessage) {
    context.system.eventStream.publish(em.event)
  }


}
