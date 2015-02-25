package pl.newicom.dddd.process

import akka.actor.ActorPath
import pl.newicom.dddd.delivery.AtLeastOnceDeliverySupport
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}

import scala.concurrent.duration._

class SagaManager(sagaConfig: SagaConfig[_], sagaOffice: ActorPath) extends AtLeastOnceDeliverySupport {
  this: EventStreamSubscriber =>

  override val destination = sagaOffice
  override def persistenceId: String = s"SagaManager-${sagaConfig.bpsName}"
  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  override def recoveryCompleted(): Unit =
    subscribe(sagaConfig.bpsName, lastSentDeliveryId)

  override def receiveCommand: Receive =
    receiveEvent(metaDataProvider).orElse(deliveryStateReceive).orElse {
      case other =>
        log.warning(s"RECEIVED: $other")
    }

  override def eventReceived(em: EventMessage, position: Long): Unit =
    persist(em, deliveryId = position)

  protected def metaDataProvider(em: EventMessage): Option[MetaData] =
    sagaConfig.correlationIdResolver.lift(em.event).map { correlationId =>
      new MetaData(Map(CorrelationId -> correlationId))
    }

}
