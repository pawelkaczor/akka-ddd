package pl.newicom.dddd.process

import akka.actor.ActorRef
import pl.newicom.dddd.office.{LocalOfficeId, OfficeRef, OfficeListener}

class CoordinationOfficeListener[E <: Saga : LocalOfficeId : ReceptorActorFactory] extends OfficeListener[E] {

  override def officeStarted(office: OfficeRef): Unit = {
    val coordinationOffice = office.asInstanceOf[CoordinationOffice[E]]
    val receptorFactory = implicitly[ReceptorActorFactory[E]]
    officeStarted(coordinationOffice, receptorFactory(coordinationOffice.receptorConfig))
  }

  def officeStarted(office: CoordinationOffice[E], receptor: ActorRef): Unit = {
    // do nothing
  }
}
