package pl.newicom.dddd.cluster

import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.cluster.DistributionStrategy.{EntityIdResolver, ExtractShardId}

object DistributionStrategy {
  type ExtractShardId = Any ⇒ String
  type EntityIdResolver = PartialFunction[Any, EntityId]

}

abstract class DistributionStrategy extends Function[EntityIdResolver, ExtractShardId]
