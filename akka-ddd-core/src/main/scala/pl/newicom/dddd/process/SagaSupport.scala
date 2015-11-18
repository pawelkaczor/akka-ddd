package pl.newicom.dddd.process

import akka.actor.{ActorRef, Props}
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, CreationSupport}
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.office.{SagaOffice, SagaConfig, OfficeFactory}

import scala.reflect.ClassTag

object SagaSupport {

  type SagaManagerFactory[E <: Saga] = SagaOffice[E] => SagaManager[E]

  implicit def defaultCaseIdResolution[E <: Saga](): EntityIdResolution[E] = new EntityIdResolution[E]

  def registerSaga[E <: Saga](office: SagaOffice[E])(implicit cs: CreationSupport, smf: SagaManagerFactory[E]): ActorRef = {
    val sagaManagerProps = Props[SagaManager[E]](smf(office))
    val sagaManager = cs.createChild(sagaManagerProps, s"SagaManager-${office.id}")

    sagaManager
  }

  def registerSaga[E <: Saga : SagaConfig : EntityIdResolution : OfficeFactory : BusinessEntityActorFactory: ClassTag]
    (implicit cs: CreationSupport, smf: SagaManagerFactory[E]): (SagaOffice[E], ActorRef) = {
    
    val sagaOffice = OfficeFactory.sagaOffice[E]
    (sagaOffice, registerSaga(sagaOffice))
  }

}
