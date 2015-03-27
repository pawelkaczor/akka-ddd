package pl.newicom.dddd.process

import akka.actor.{ActorPath, ActorRef, Props}
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, CreationSupport}
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.office.{Office, OfficeFactory}

object SagaSupport {

  /**
   * Responsible of creating [[SagaManager]] using provided [[SagaConfig]] and path to saga office
   */
  type SagaManagerFactory = (SagaConfig[_], ActorPath) => SagaManager

  implicit def defaultCaseIdResolution[A <: Saga](): EntityIdResolution[A] = new EntityIdResolution[A]

  def registerSaga[A <: Saga : SagaConfig](sagaOffice: ActorRef)(implicit cs: CreationSupport, smf: SagaManagerFactory): ActorRef = {
    val sagaOfficePath = sagaOffice.path
    val sagaConfig: SagaConfig[A] = implicitly[SagaConfig[A]]

    val sagaManagerProps = Props[SagaManager](smf(sagaConfig, sagaOfficePath))
    val sagaManager = cs.createChild(sagaManagerProps, s"SagaManager-${sagaConfig.bpsName}")

    sagaManager
  }

  def registerSaga[A <: Saga : SagaConfig : EntityIdResolution : OfficeFactory : BusinessEntityActorFactory]
    (implicit cs: CreationSupport, smf: SagaManagerFactory): (ActorRef, ActorRef) = {
    
    val sagaOffice = Office.office[A]
    (sagaOffice, registerSaga[A](sagaOffice))
  }

}
