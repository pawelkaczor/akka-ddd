package pl.newicom.dddd.process

import akka.actor.{ActorRef, Props}
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.process.SagaSupport.SagaManagerFactory
import pl.newicom.dddd.saga.SagaOffice

trait SagaSupport {

  implicit def sagaOfficeListener[E <: Saga : SagaManagerFactory](implicit cs: CreationSupport): SagaOfficeListener[E] =
    new SagaOfficeListener[E]

}

object SagaSupport {

  type SagaManagerFactory[E <: Saga] = SagaOffice[E] => SagaManager[E]

  implicit def defaultCaseIdResolution[E <: Saga](): EntityIdResolution[E] = new EntityIdResolution[E]


  def sagaManager[E <: Saga](office: SagaOffice[E])(implicit cs: CreationSupport, smf: SagaManagerFactory[E]): ActorRef = {
    val sagaManagerProps = Props[SagaManager[E]](smf(office))
    cs.createChild(sagaManagerProps, s"SagaManager-${office.id}")
  }

}