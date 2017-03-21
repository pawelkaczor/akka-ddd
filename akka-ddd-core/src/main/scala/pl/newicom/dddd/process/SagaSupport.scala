package pl.newicom.dddd.process

import pl.newicom.dddd.office.LocalOfficeId

trait SagaSupport {

  implicit def officeListener[E <: Saga : LocalOfficeId : ReceptorActorFactory]: CoordinationOfficeListener[E] =
    new CoordinationOfficeListener[E]

}

