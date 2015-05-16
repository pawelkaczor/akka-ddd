package pl.newicom.dddd.test.dummy

import akka.actor.Actor
import pl.newicom.dddd.test.dummy.ValueGenerator.{ValueGenerated, GenerateRandom}
import pl.newicom.dddd.utils.UUIDSupport

object ValueGenerator {
  case object GenerateRandom
  case class ValueGenerated(value: String)
}

class ValueGenerator extends Actor with UUIDSupport {
  override def receive: Receive = {
    case GenerateRandom => sender() ! ValueGenerated(uuid)
  }
}
