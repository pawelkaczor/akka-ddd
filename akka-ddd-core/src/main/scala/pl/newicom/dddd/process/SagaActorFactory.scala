package pl.newicom.dddd.process

import pl.newicom.dddd.actor.BusinessEntityActorFactory

import scala.concurrent.duration._

abstract class SagaActorFactory[A <: Saga : ProcessConfig] extends BusinessEntityActorFactory[A] {

  def inactivityTimeout: Duration = 1.minute
  def config: ProcessConfig[A] = implicitly[ProcessConfig[A]]
}
