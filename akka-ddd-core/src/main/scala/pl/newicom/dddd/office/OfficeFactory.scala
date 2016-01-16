package pl.newicom.dddd.office

import akka.actor.ActorRef
import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.correlation.EntityIdResolution

import scala.reflect.ClassTag

object OfficeFactory {

  def office[A <: BusinessEntity : BusinessEntityActorFactory : EntityIdResolution : OfficeFactory : LocalOfficeId: ClassTag]: Office[A] = {
    Office(implicitly[LocalOfficeId[A]], implicitly[OfficeFactory[A]].getOrCreate())
  }

  def sagaOffice[A <: BusinessEntity : BusinessEntityActorFactory : EntityIdResolution : OfficeFactory : SagaConfig: ClassTag]: SagaOffice[A] = {
    new SagaOffice(implicitly[SagaConfig[A]], implicitly[OfficeFactory[A]].getOrCreate())
  }

}

abstract class OfficeFactory[A <: BusinessEntity : BusinessEntityActorFactory : EntityIdResolution: LocalOfficeId] {
  def officeId = implicitly[LocalOfficeId[A]]
  def getOrCreate(): ActorRef
}