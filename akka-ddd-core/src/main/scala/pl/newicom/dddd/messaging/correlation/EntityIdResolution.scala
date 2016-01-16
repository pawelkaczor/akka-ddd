package pl.newicom.dddd.messaging.correlation

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.messaging.AddressableMessage
import pl.newicom.dddd.messaging.correlation.EntityIdResolution.EntityIdResolver

object EntityIdResolution {
  type EntityIdResolver = PartialFunction[Any, EntityId]
}

class EntityIdResolution[A] {

  def entityIdResolver: EntityIdResolver = {
    case msg: AddressableMessage => msg.destination.get
  }
}
