package pl.newicom.dddd.messaging.correlation

import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.cluster.DistributionStrategy.EntityIdResolver

class AggregateIdResolution extends EntityIdResolution {

  override def entityIdResolver: EntityIdResolver = {
    super.entityIdResolver.orElse {
      case c: Command => c.aggregateId
    }
  }
}

