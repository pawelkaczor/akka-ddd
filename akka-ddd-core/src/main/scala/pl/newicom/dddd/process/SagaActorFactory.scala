package pl.newicom.dddd.process

import akka.actor.Props
import pl.newicom.dddd.actor.{PassivationConfig, BusinessEntityActorFactory}

abstract class SagaActorFactory[A <: Saga] extends BusinessEntityActorFactory[A] {
  import scala.concurrent.duration._

  def props(pc: PassivationConfig): Props
  def inactivityTimeout: Duration = 1.minute
}
