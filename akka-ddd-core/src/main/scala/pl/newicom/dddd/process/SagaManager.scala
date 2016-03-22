package pl.newicom.dddd.process

import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventStreamSubscriber, OfficeEventMessage}
import pl.newicom.dddd.saga.SagaOffice

import scala.concurrent.duration._

class SagaManager[E <: Saga](implicit val sagaOffice: SagaOffice[E]) extends Receptor {
  this: EventStreamSubscriber =>

  def defaultConfig: ReceptorConfig =
    ReceptorBuilder()
      .reactTo(sagaOffice.businessProcess)
      .propagateTo(sagaOffice.actorPath)


  lazy val config = defaultConfig

  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  override def metaDataProvider(em: OfficeEventMessage): Option[MetaData] =
    sagaOffice.config.correlationIdResolver.lift(em.event).map { correlationId =>
      MetaData(Map(
        CorrelationId -> correlationId
      ))
    }

  override def recoveryCompleted(): Unit = {
    super.recoveryCompleted()
    log.info(s"SagaManager: $persistenceId for Saga office: ${sagaOffice.actorPath} is up and running.")
  }
}
