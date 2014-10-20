package pl.newicom.dddd.messaging.correlation

import pl.newicom.dddd.aggregate.BusinessEntity._
import pl.newicom.dddd.messaging.EntityMessage
import pl.newicom.dddd.messaging.correlation.EntityIdResolution.EntityIdResolver

object EntityIdResolution {
  type EntityIdResolver = PartialFunction[Any, EntityId]
}

class EntityIdResolution[A] {

  def entityIdResolver: EntityIdResolver = {
    case em: EntityMessage => em.entityId
  }
}
