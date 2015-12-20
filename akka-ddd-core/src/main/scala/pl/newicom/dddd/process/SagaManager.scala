package pl.newicom.dddd.process

import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}
import pl.newicom.dddd.office.SagaOffice

import scala.concurrent.duration._

class SagaManager[E <: Saga](implicit val sagaOffice: SagaOffice[E]) extends Receptor {
  this: EventStreamSubscriber =>

  lazy val config: ReceptorConfig =
    ReceptorBuilder()
      .reactTo(sagaOffice.bps)
      .propagateTo(sagaOffice.actor.path)

  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  def metaDataProvider(em: EventMessage): Option[MetaData] =
    sagaOffice.config.correlationIdResolver.lift(em.event).map { correlationId =>
      new MetaData(Map(CorrelationId -> correlationId))
    }

}
