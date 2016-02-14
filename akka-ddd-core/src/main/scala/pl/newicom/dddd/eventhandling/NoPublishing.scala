package pl.newicom.dddd.eventhandling

import akka.actor.Actor
import pl.newicom.dddd.messaging.event.OfficeEventMessage

trait NoPublishing extends EventPublisher {
  this: Actor =>

  override def publish(em: OfficeEventMessage): Unit = ()

}
