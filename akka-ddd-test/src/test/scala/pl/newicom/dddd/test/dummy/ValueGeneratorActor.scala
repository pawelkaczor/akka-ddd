package pl.newicom.dddd.test.dummy

import akka.actor.{Props, Actor}
import pl.newicom.dddd.test.dummy.ValueGeneratorActor.{GenerateRandom, ValueGenerated}

object ValueGeneratorActor {
  case object GenerateRandom
  case class ValueGenerated(value: Int)

  def props(generator: => Int) = Props(new ValueGeneratorActor(generator))
}

class ValueGeneratorActor(generator: => Int) extends Actor {
  override def receive: Receive = {
    case GenerateRandom => sender() ! ValueGenerated(generator)
  }
}
