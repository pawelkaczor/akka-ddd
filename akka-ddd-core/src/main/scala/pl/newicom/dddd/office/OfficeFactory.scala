package pl.newicom.dddd.office

import akka.actor.ActorRef
import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.correlation.EntityIdResolution

import scala.reflect.ClassTag

object OfficeFactory {
  def office[A <: BusinessEntity : BusinessEntityActorFactory : EntityIdResolution : OfficeFactory]: ActorRef = {
    implicitly[OfficeFactory[A]].getOrCreate
  }
}

abstract class OfficeFactory[A <: BusinessEntity : BusinessEntityActorFactory : EntityIdResolution : ClassTag] {

  def getOrCreate: ActorRef

  def officeName = implicitly[ClassTag[A]].runtimeClass.getSimpleName
}