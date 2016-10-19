package pl.newicom.dddd.process

import akka.actor.ActorRef
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.office.{Office, OfficeListener}
import pl.newicom.dddd.process.ReceptorSupport.{ReceptorFactory, receptor}
import pl.newicom.dddd.saga.CoordinationOffice

class CoordinationOfficeListener[E <: Saga](implicit cs: CreationSupport, rf: ReceptorFactory) extends OfficeListener[E] {

  override def officeStarted(office: Office): Unit = {
    val coordinationOffice = office.asInstanceOf[CoordinationOffice[E]]
    officeStarted(coordinationOffice, receptor(coordinationOffice.receptorConfig))
  }

  def officeStarted(office: CoordinationOffice[E], receptor: ActorRef): Unit = {
    // do nothing
  }
}
