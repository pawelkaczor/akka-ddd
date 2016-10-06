package pl.newicom.dddd.cluster

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.sharding.ShardRegion.Passivate
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, PassivationConfig}
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.office.{LocalOfficeId, OfficeFactory, OfficeId}

trait ShardingSupport {

  implicit def globalOfficeFactory[A <: BusinessEntity : ShardResolution : BusinessEntityActorFactory : LocalOfficeId](implicit system: ActorSystem): OfficeFactory[A] = {
    new OfficeFactory[A] {

      val shardSettings = ClusterShardingSettings(system).withRole(officeId.department)

      override def getOrCreate(): ActorRef = {
        region(officeId).getOrElse {
          startSharding(shardSettings)
          region(officeId).get
        }
      }

      private def startSharding(shardSettings: ClusterShardingSettings): Unit = {
        val entityFactory = implicitly[BusinessEntityActorFactory[A]]
        val entityProps = entityFactory.props(PassivationConfig(Passivate(PoisonPill), entityFactory.inactivityTimeout))
        val sr = implicitly[ShardResolution[A]]

        ClusterSharding(system).start(
          typeName = officeId.id,
          entityProps = entityProps,
          settings = shardSettings,
          extractEntityId = sr.idExtractor,
          extractShardId = sr.shardResolver)

          startClusterClientReceptionist(officeId)
      }

    }
  }

  def proxy(officeId: OfficeId, sr: ShardResolution[_] = new DefaultShardResolution)(implicit system: ActorSystem): ActorRef = {

    def startProxy(): Unit = {
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

  private def startClusterClientReceptionist(officeId: OfficeId)(implicit as: ActorSystem): Unit = {
    ClusterClientReceptionist(as).registerService(region(officeId).get)
  }

  private def region(officeId: OfficeId)(implicit as: ActorSystem): Option[ActorRef] = {
    try {
      Some(ClusterSharding(as).shardRegion(officeId.id))
    } catch {
      case ex: IllegalArgumentException => None
    }
  }

}