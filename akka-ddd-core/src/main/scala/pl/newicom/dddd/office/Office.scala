package pl.newicom.dddd.office

import akka.actor.ActorRef
import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.correlation.EntityIdResolution

object Office {

  def office[A <: BusinessEntity : BusinessEntityActorFactory : EntityIdResolution : OfficeFactory]: ActorRef = {
    implicitly[OfficeFactory[A]].getOrCreate
  }

}
