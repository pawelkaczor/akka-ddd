package pl.newicom.dddd.messaging.correlation

import pl.newicom.dddd.cluster.DistributionStrategy.EntityIdResolver
import pl.newicom.dddd.messaging.AddressableMessage

class EntityIdResolution {
  def entityIdResolver: EntityIdResolver = {
    case msg: AddressableMessage => msg.destination.get
  }
}
