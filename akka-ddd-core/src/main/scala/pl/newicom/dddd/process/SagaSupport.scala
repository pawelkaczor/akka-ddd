package pl.newicom.dddd.process

import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.process.ReceptorSupport.ReceptorFactory

trait SagaSupport {

  implicit def officeListener[E <: Saga](implicit cs: CreationSupport, rf: ReceptorFactory): CoordinationOfficeListener[E] =
    new CoordinationOfficeListener[E]

}

