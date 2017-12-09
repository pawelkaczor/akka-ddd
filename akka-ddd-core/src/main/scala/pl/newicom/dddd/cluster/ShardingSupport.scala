package pl.newicom.dddd.cluster

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.sharding.ShardRegion.Passivate
import pl.newicom.dddd.BusinessEntity
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, PassivationConfig}
import pl.newicom.dddd.office.{LocalOfficeId, OfficeFactory, OfficeId}

trait ShardingSupport {

  implicit def distributedOfficeFactory[A <: BusinessEntity : BusinessEntityActorFactory : LocalOfficeId](implicit as: ActorSystem): OfficeFactory[A] = {
    new OfficeFactory[A] {

      val shardSettings: ClusterShardingSettings = ClusterShardingSettings(as).withRole(officeId.department)

      override def getOrCreate(): ActorRef = {
        region(officeId).getOrElse {
          startSharding(shardSettings)
          region(officeId).get
        }
      }

      private def startSharding(shardSettings: ClusterShardingSettings): Unit = {
        val entityFactory = implicitly[BusinessEntityActorFactory[A]]
        val entityProps = entityFactory.props(PassivationConfig(Passivate(PoisonPill), entityFactory.inactivityTimeout))
        val sr = shardResolution(officeId)

        ClusterSharding(as).start(
          typeName = officeId.id,
          entityProps = entityProps,
          settings = shardSettings,
          extractEntityId = sr.idExtractor,
          extractShardId = sr.shardResolver)

          startClusterClientReceptionist(officeId)
      }

    }
  }

  def proxy(officeId: OfficeId)(implicit system: ActorSystem): ActorRef = {

    def startProxy(): Unit = {
      val sr = shardResolution(officeId)

      ClusterSharding(system).startProxy(
        typeName = officeId.id,
        role = Some(officeId.department),
        extractEntityId = sr.idExtractor,
        extractShardId = sr.shardResolver)

      startClusterClientReceptionist(officeId)

    }

    region(officeId).getOrElse {
      startProxy()
      region(officeId).get
    }

  }

  private def shardResolution(officeId: OfficeId): ShardResolution =
    new ShardResolution(officeId.distributionStrategy)

  private def startClusterClientReceptionist(officeId: BusinessEntity)(implicit as: ActorSystem): Unit = {
    ClusterClientReceptionist(as).registerService(region(officeId).get)
  }

  private def region(officeId: BusinessEntity)(implicit as: ActorSystem): Option[ActorRef] = {
    try {
      Some(ClusterSharding(as).shardRegion(officeId.id))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
}