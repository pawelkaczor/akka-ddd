package pl.newicom.dddd.cluster

import pl.newicom.dddd.cluster.ShardResolution.ShardResolutionStrategy
import pl.newicom.dddd.messaging.correlation.AggregateIdResolution

class DefaultShardResolution[A] extends AggregateIdResolution[A] with ShardResolution[A] {

  val defaultShardResolutionStrategy: ShardResolutionStrategy = {
    entityIdResolver =>
    {
      case msg => Integer.toHexString(entityIdResolver(msg).hashCode).charAt(0).toString
    }
  }

  def shardResolutionStrategy = defaultShardResolutionStrategy


}
