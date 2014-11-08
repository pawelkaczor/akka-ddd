package pl.newicom.dddd.messaging.correlation

import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.messaging.correlation.EntityIdResolution.EntityIdResolver

class AggregateIdResolution[A] extends EntityIdResolution[A] {

  override def entityIdResolver: EntityIdResolver = {
    super.entityIdResolver.orElse {
      case c: Command => c.aggregateId
    }
  }
}

