package pl.newicom.dddd.eventhandling

import akka.actor.{Actor, Props}
import pl.newicom.dddd.aggregate.AggregateRoot.DomainEvent
import pl.newicom.dddd.delivery.DeliveryContext._
import pl.newicom.dddd.messaging.event.DomainEventMessage
import pl.newicom.dddd.serialization.SerializationSupportForActor

object ReliableEventHandler {

  def props(handler: DomainEvent => Unit) = Props(ReliableEventHandler(handler))

  def apply(handler: DomainEvent => Unit) = new ReliableEventHandler {
    override def handle(event: DomainEvent): Unit = handler(event)
  }
}

abstract class ReliableEventHandler extends Actor with SerializationSupportForActor {

  override def receive: Receive = {
    case em: DomainEventMessage =>
      handle(em.event)
      em.confirmIfRequested()

  }

  def handle(event: DomainEvent)
}
