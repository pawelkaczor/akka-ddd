package pl.newicom.dddd.cluster

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding}
import akka.cluster.sharding.ShardRegion.Passivate
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, PassivationConfig}
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.office.OfficeFactory

import scala.reflect.ClassTag

trait ShardingSupport {

  implicit def globalOfficeFactory[A <: BusinessEntity : ShardResolution : BusinessEntityActorFactory : ClassTag](implicit system: ActorSystem): OfficeFactory[A] = {
    new OfficeFactory[A] {

      val shardSettings = ClusterShardingSettings(system)

      private def region: Option[ActorRef] = {
        try {
          Some(ClusterSharding(system).shardRegion(officeName))
        } catch {
          case ex: IllegalArgumentException => None
        }
      }

      override def getOrCreate: ActorRef = {
        region.getOrElse {
          startSharding(shardSettings)
          region.get
        }
      }

      private def startSharding(shardSettings: ClusterShardingSettings): Unit = {
        val entityFactory = implicitly[BusinessEntityActorFactory[A]]
        val entityProps = entityFactory.props(new PassivationConfig(Passivate(PoisonPill), entityFactory.inactivityTimeout))
        val entityClass = implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]
        val sr = implicitly[ShardResolution[A]]

        ClusterSharding(system).start(
          typeName = entityClass.getSimpleName,
          entityProps = entityProps,
          settings = shardSettings,
          extractEntityId = sr.idExtractor,
          extractShardId = sr.shardResolver)

        ClusterClientReceptionist(system).registerService(region.get)

      }

    }

  }

}