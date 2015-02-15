package pl.newicom.dddd.process

import akka.actor.{ActorLogging, ActorPath}
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import pl.newicom.dddd.delivery.protocol.alod.Delivered
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}

import scala.collection.immutable.SortedMap
import scala.concurrent.duration._
import scala.util.Try

// State
case class SagaManagerState(
    lastConfirmedPosition: Option[Long] = None,
    // position -> internal deliveryID
    unconfirmedPositions: SortedMap[Long, Long] = SortedMap.empty[Long, Long]) {

  def internalDeliveryId(position: Long) = unconfirmedPositions.get(position)
  
  def withEventSent(internalDeliveryId: Long, pos: Long) =
    SagaManagerState(lastConfirmedPosition, unconfirmedPositions.updated(pos, internalDeliveryId))

  def withEventDelivered(pos: Long) =
    SagaManagerState(Some(pos), unconfirmedPositions - pos)

  def nextSubscribePosition: Option[Long] = firstUnconfirmedPosition.orElse(lastConfirmedPosition)

  private def firstUnconfirmedPosition: Option[Long] = Try(unconfirmedPositions.lastKey).toOption

}

// Snapshot
case class SagaManagerSnapshot(state: SagaManagerState, alodSnapshot: AtLeastOnceDeliverySnapshot)

import akka.persistence._

class SagaManager(sagaConfig: SagaConfig[_], sagaOffice: ActorPath) extends PersistentActor with AtLeastOnceDelivery with ActorLogging {
  this: EventStreamSubscriber =>

  private var state = SagaManagerState()

  def bpsName = sagaConfig.bpsName
  def correlationIdResolver = sagaConfig.correlationIdResolver

  override def persistenceId: String = s"SagaManager-$bpsName"

  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  def metaDataProvider(em: EventMessage) = Some(new MetaData(Map(
    CorrelationId -> correlationIdResolver(em.event)
  )))

  override def eventReceived(em: EventMessage): Unit = {
    persist(em)(updateState)
  }

  override def receiveRecover: Receive = {
    case RecoveryCompleted  =>
      log.debug("Recovery completed")
      subscribe(bpsName, state.nextSubscribePosition)

    case SnapshotOffer(metadata, SagaManagerSnapshot(smState, alodSnapshot)) =>
      setDeliverySnapshot(alodSnapshot)
      state = smState
      log.debug(s"Snapshot restored: ${state.unconfirmedPositions}")

    case msg =>
      updateState(msg)
  }

  override def receiveCommand: Receive = receiveEvent(metaDataProvider).orElse {
    case receipt: Delivered =>
      persist(receipt)(updateState)

    case "snap" =>
      val snapshot = new SagaManagerSnapshot(state, getDeliverySnapshot)
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
      val pos = eventPosition(em)
      deliver(sagaOffice, internalDeliveryId => {
        log.debug(s"[DELIVERY-ID: $pos] Delivering: $em")
        state = state.withEventSent(internalDeliveryId, pos)
        em.withMetaAttribute(DeliveryId, pos)
      })

    case receipt: Delivered =>
      val pos = receipt.deliveryId
      state.internalDeliveryId(pos).foreach { internalDeliveryId =>
        log.debug(s"[DELIVERY-ID: $pos] - Delivery confirmed")
        if (confirmDelivery(internalDeliveryId)) {
          state = state.withEventDelivered(pos)
        }
      }

  }


}
