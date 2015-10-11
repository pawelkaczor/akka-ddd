package pl.newicom.dddd.process

import akka.actor.ActorPath
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}
import pl.newicom.dddd.office.OfficeInfo
import SagaManager._
import scala.concurrent.duration._

class BusinessProcess[A : OfficeInfo]

object SagaManager {
  implicit def businessProcessInfo(implicit sc: SagaConfig[_]): OfficeInfo[BusinessProcess[_]] = {
    new OfficeInfo[BusinessProcess[_]] {
      def name = sc.name
      override def isSagaOffice = true
    }
  }

}

class SagaManager(sagaConfig: SagaConfig[_], sagaOffice: ActorPath) extends Receptor {
  this: EventStreamSubscriber =>

  implicit val sc: SagaConfig[_] = sagaConfig

  lazy val config: ReceptorConfig =
    ReceptorBuilder().
      reactTo[BusinessProcess[_]].
      propagateTo(sagaOffice)
  
  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  override def metaDataProvider(em: EventMessage): Option[MetaData] =
    sagaConfig.correlationIdResolver.lift(em.event).map { correlationId =>
      new MetaData(Map(CorrelationId -> correlationId))
    }

}
