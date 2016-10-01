package pl.newicom.dddd.process

import akka.actor.ActorRef
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.office.{Office, OfficeListener}
import pl.newicom.dddd.process.SagaSupport.{SagaManagerFactory, sagaManager}
import pl.newicom.dddd.saga.SagaOffice

class SagaOfficeListener[E <: Saga : SagaManagerFactory](implicit cs: CreationSupport) extends OfficeListener[E] {

  override def officeStarted(office: Office): Unit = {
    val sagaOffice = office.asInstanceOf[SagaOffice[E]]
    officeStarted(sagaOffice, sagaManager(sagaOffice))
  }

  def officeStarted(office: SagaOffice[E], sagaManager: ActorRef): Unit = {
    // do nothing
  }
}
