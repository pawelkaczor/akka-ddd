package pl.newicom.dddd.process

import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.office.LocalOfficeId

import scala.concurrent.duration._

abstract class SagaActorFactory[A <: Saga : LocalOfficeId] extends BusinessEntityActorFactory[A] {

  def inactivityTimeout: Duration = 1.minute
  def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
}
