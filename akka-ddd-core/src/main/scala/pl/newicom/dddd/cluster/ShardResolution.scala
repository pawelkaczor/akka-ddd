package pl.newicom.dddd.cluster

import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.cluster.DistributionStrategy.ExtractShardId
import pl.newicom.dddd.cluster.ShardResolution.ExtractEntityId
import pl.newicom.dddd.messaging.AddressableMessage
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.correlation.EntityIdResolution

object ShardResolution {
  type ExtractEntityId = PartialFunction[Any, (String, Any)]
}

class ShardResolution(strategy: DistributionStrategy) extends EntityIdResolution {

  def shardResolver: ExtractShardId =
    strategy(entityIdResolver)

  def idExtractor: ExtractEntityId = {
    case msg: AddressableMessage => (entityIdResolver(msg), msg)
    case c: Command => (entityIdResolver(c), CommandMessage(c))
  }

}

