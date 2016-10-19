package pl.newicom.dddd.process

import akka.actor.ActorRef
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.office.{Office, OfficeListener}
import pl.newicom.dddd.process.SagaSupport.{ReceptorFactory, receptor}
import pl.newicom.dddd.saga.CoordinationOffice

class CoordinationOfficeListener[E <: Saga : ReceptorFactory](implicit cs: CreationSupport) extends OfficeListener[E] {

  override def officeStarted(office: Office): Unit = {
    val processOffice = office.asInstanceOf[CoordinationOffice[E]]
    officeStarted(processOffice, receptor(processOffice))
  }

  def officeStarted(office: CoordinationOffice[E], processReceptor: ActorRef): Unit = {
    // do nothing
  }
}
