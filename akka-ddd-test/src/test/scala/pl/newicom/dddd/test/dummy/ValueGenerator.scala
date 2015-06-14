package pl.newicom.dddd.test.dummy

import akka.actor.Actor
import pl.newicom.dddd.test.dummy.ValueGenerator.{GenerateRandom, ValueGenerated}

object ValueGenerator {
  case object GenerateRandom
  case class ValueGenerated(value: Int)
}

class ValueGenerator extends Actor {
  override def receive: Receive = {
    case GenerateRandom => sender() ! ValueGenerated((Math.random() * 100).toInt)
  }
}
