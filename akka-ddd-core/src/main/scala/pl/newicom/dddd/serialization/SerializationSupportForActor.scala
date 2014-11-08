package pl.newicom.dddd.serialization

import akka.actor.Actor

trait SerializationSupportForActor extends SerializationSupport {
  this: Actor =>

  implicit override def system = context.system
}
