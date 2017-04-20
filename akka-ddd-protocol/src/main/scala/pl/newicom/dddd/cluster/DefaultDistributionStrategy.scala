package pl.newicom.dddd.cluster

import pl.newicom.dddd.cluster.DistributionStrategy.{EntityIdResolver, ExtractShardId}

class DefaultDistributionStrategy extends DistributionStrategy {

  def apply(entityIdResolver: EntityIdResolver): ExtractShardId = {
    msg => Integer.toHexString(entityIdResolver(msg).hashCode).charAt(0).toString
  }

}
