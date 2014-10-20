package pl.newicom.dddd.cluster

import akka.contrib.pattern.ShardRegion._
import pl.newicom.dddd.cluster.ShardResolution._
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.messaging.EntityMessage
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.messaging.correlation.EntityIdResolution.EntityIdResolver

object ShardResolution {
  type ShardResolutionStrategy = EntityIdResolver => ShardResolver
}

trait ShardResolution[A] extends EntityIdResolution[A] {

  def shardResolutionStrategy: ShardResolutionStrategy

  val shardResolver: ShardResolver = shardResolutionStrategy(entityIdResolver)

  val idExtractor: IdExtractor = {
    case em: EntityMessage => (entityIdResolver(em), em)
    case c: Command => (entityIdResolver(c), CommandMessage(c))
  }

}

