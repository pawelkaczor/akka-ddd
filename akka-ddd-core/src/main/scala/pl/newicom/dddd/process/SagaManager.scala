package pl.newicom.dddd.process

import akka.actor.{ActorLogging, ActorPath}
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import pl.newicom.dddd.delivery.AtLeastOnceDeliveryState
import pl.newicom.dddd.delivery.protocol.alod.Delivered
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}
import akka.persistence._
import scala.concurrent.duration._

case class SagaManagerSnapshot(state: AtLeastOnceDeliveryState, alodSnapshot: AtLeastOnceDeliverySnapshot)

class SagaManager(sagaConfig: SagaConfig[_], sagaOffice: ActorPath) extends PersistentActor with AtLeastOnceDelivery with ActorLogging {
  this: EventStreamSubscriber =>

  private var alodState = AtLeastOnceDeliveryState()

  def bpsName = sagaConfig.bpsName
  def correlationIdResolver = sagaConfig.correlationIdResolver.lift

  override def persistenceId: String = s"SagaManager-$bpsName"

  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  def nextSubscribePosition = alodState.oldestUnconfirmed.orElse(alodState.recentlyConfirmed)
  
  def metaDataProvider(em: EventMessage) = correlationIdResolver(em.event).map {
    correlationId => new MetaData(Map(CorrelationId -> correlationId))
  }

  override def eventReceived(em: EventMessage, position: Long): Unit = {
    persist(em.withMetaAttribute(DeliveryId, position))(updateState)
  }

  override def receiveRecover: Receive = {
    case RecoveryCompleted  =>
      log.debug("Recovery completed")
      subscribe(bpsName, nextSubscribePosition)

    case SnapshotOffer(metadata, SagaManagerSnapshot(smState, alodSnapshot)) =>
      setDeliverySnapshot(alodSnapshot)
      alodState = smState
      log.debug(s"Snapshot restored: ${alodState.unconfirmed}")

    case msg =>
      updateState(msg)
  }

  override def receiveCommand: Receive = receiveEvent(metaDataProvider).orElse {
    case receipt: Delivered =>
      persist(receipt)(updateState)

    case "snap" =>
      val snapshot = new SagaManagerSnapshot(alodState, getDeliverySnapshot)
      log.debug(s"Saving snapshot: $snapshot")
      saveSnapshot(snapshot)

    case SaveSnapshotSuccess(metadata) =>
      log.debug("Snapshot saved successfully")

    case f @ SaveSnapshotFailure(metadata, reason) =>
      log.error(s"$f")

    case other =>
      log.warning(s"RECEIVED: $other")
  }

  def updateState(msg: Any): Unit = msg match {
    case em: EventMessage =>
      if (em.entityId == null) {
        log.warning(s"No correlationId. Skipping $em")
      } else {
        deliver(sagaOffice, internalDeliveryId => {
          val deliveryId = em.getMetaAttribute[Any](DeliveryId) match {
            case bigInt: BigInt => bigInt.toLong
            case l => l.asInstanceOf[Long]
          }
          log.debug(s"[DELIVERY-ID: $deliveryId] Delivering: $em")
          alodState = alodState.withSent(internalDeliveryId, deliveryId)
          em
        })
      }

    case receipt: Delivered =>
      val deliveryId = receipt.deliveryId
      alodState.internalDeliveryId(deliveryId).foreach { internalDeliveryId =>
        log.debug(s"[DELIVERY-ID: $deliveryId] - Delivery confirmed")
        if (confirmDelivery(internalDeliveryId)) {
          alodState = alodState.withDelivered(deliveryId)
        }
      }

  }


}
