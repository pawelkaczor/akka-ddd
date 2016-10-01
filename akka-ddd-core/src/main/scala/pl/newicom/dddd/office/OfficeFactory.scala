package pl.newicom.dddd.office

import akka.actor.ActorRef
import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.saga.{SagaConfig, SagaOffice}

import scala.reflect.ClassTag

object OfficeFactory {

  def office[A <: BusinessEntity : BusinessEntityActorFactory : EntityIdResolution : OfficeFactory : LocalOfficeId : OfficeListener : ClassTag]: Office = {

    val officeId = implicitly[LocalOfficeId[A]]
    val actor = implicitly[OfficeFactory[A]].getOrCreate()

    val office = officeId match {
      case sc: SagaConfig[A]    =>   new SagaOffice[A](sc, actor)
      case LocalOfficeId(_, _)  =>   new Office(officeId, actor)
    }

    implicitly[OfficeListener[A]].officeStarted(office)
    office
  }

}

abstract class OfficeFactory[A <: BusinessEntity : BusinessEntityActorFactory : EntityIdResolution: LocalOfficeId] {
  def officeId = implicitly[LocalOfficeId[A]]
  def getOrCreate(): ActorRef
}