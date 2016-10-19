package pl.newicom.dddd.process

import akka.actor.{ActorRef, Props}
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.process.SagaSupport.ReceptorFactory
import pl.newicom.dddd.saga.CoordinationOffice

trait SagaSupport {

  implicit def officeListener[E <: Saga : ReceptorFactory](implicit cs: CreationSupport): CoordinationOfficeListener[E] =
    new CoordinationOfficeListener[E]

}

object SagaSupport {

  type ReceptorFactory[E <: Saga] = CoordinationOffice[E] => Receptor

  def receptor[E <: Saga](office: CoordinationOffice[E])(implicit cs: CreationSupport, rf: ReceptorFactory[E]): ActorRef = {
    cs.createChild(Props(rf(office)), s"Receptor-${office.id}")
  }

}