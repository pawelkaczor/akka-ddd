package pl.newicom.dddd.cluster

import akka.actor._
import akka.contrib.pattern.{ClusterReceptionistExtension, ClusterSharding}
import akka.contrib.pattern.ShardRegion.Passivate
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, PassivationConfig}
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.office.OfficeFactory

import scala.reflect.ClassTag

trait ShardingSupport {

  implicit def globalOfficeFactory[A <: BusinessEntity : ShardResolution : BusinessEntityActorFactory : ClassTag](implicit system: ActorSystem): OfficeFactory[A] = {
    new OfficeFactory[A] {
      private def region: Option[ActorRef] = {
        try {
          Some(ClusterSharding(system).shardRegion(officeName))
        } catch {
          case ex: IllegalArgumentException => None
        }
      }
      override def getOrCreate: ActorRef = {
        region.getOrElse {
          startSharding()
          region.get
        }
      }

      private def startSharding(): Unit = {
        val entityFactory = implicitly[BusinessEntityActorFactory[A]]
        val entityProps = entityFactory.props(new PassivationConfig(Passivate(PoisonPill), entityFactory.inactivityTimeout))
        val entityClass = implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]
        val sr = implicitly[ShardResolution[A]]

        ClusterSharding(system).start(
          typeName = entityClass.getSimpleName,
          entryProps = Some(entityProps),
          idExtractor = sr.idExtractor,
          shardResolver = sr.shardResolver,
          roleOverride = None,
          rememberEntries = false)

        ClusterReceptionistExtension(system).registerService(region.get)

      }

    }

  }

}