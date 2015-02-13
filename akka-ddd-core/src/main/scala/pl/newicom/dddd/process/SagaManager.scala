package pl.newicom.dddd.process

import akka.actor.{ActorLogging, ActorPath}
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import akka.persistence._
import pl.newicom.dddd.aggregate.{DomainEvent, EntityId}
import pl.newicom.dddd.delivery.protocol.ConfirmEvent
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventStreamSubscriber, EventMessage}

import scala.collection.immutable.SortedMap
import scala.concurrent.duration._

case class EventConfirmed(deliveryId: Long, eventPosition: Long)

// State
case class SagaManagerState(
    lastConfirmedPosition: Option[Long] = None,
    unconfirmedPositionsByDeliveryId: SortedMap[Long, Long] = SortedMap.empty[Long, Long]) {

  def withEventSent(deliveryId: Long, eventPosition: Long) =
    SagaManagerState(lastConfirmedPosition, unconfirmedPositionsByDeliveryId.updated(deliveryId, eventPosition))

  def withEventConfirmed(deliveryId: Long, eventPosition: Long) =
    SagaManagerState(Some(eventPosition), unconfirmedPositionsByDeliveryId - deliveryId)

  def nextSubscribePosition: Option[Long] = firstUnconfirmedEventPosition.orElse(lastConfirmedPosition)

  private def firstUnconfirmedEventPosition: Option[Long] = {
    if (unconfirmedPositionsByDeliveryId.isEmpty) {
      None
    } else {
      val deliveryId = unconfirmedPositionsByDeliveryId.lastKey
      unconfirmedPositionsByDeliveryId.get(deliveryId)
    }
  }

}

// Snapshot
case class SagaManagerSnapshot(state: SagaManagerState, alodSnapshot: AtLeastOnceDeliverySnapshot)

trait SagaManager extends PersistentActor with AtLeastOnceDelivery with ActorLogging {
  this: EventStreamSubscriber =>

  private var state = SagaManagerState()

  def sagaOffice: ActorPath
  def bpsName: String
  def correlationIdResolver: DomainEvent => EntityId

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
      log.debug(s"Snapshot restored: ${state.unconfirmedPositionsByDeliveryId}")

    case msg =>
      updateState(msg)
  }

  override def receiveCommand: Receive = receiveEvent(metaDataProvider).orElse {
    case ConfirmEvent(deliveryId, eventPosition) =>
      persist(EventConfirmed(deliveryId, eventPosition))(updateState)

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
      deliver(sagaOffice, deliveryId => {
        log.debug(s"[DELIVERY-ID: ${(deliveryId, pos)}] Delivering: $em")
        state = state.withEventSent(deliveryId, pos)
        em.withMetaAttribute(DeliveryId, deliveryId)
      })

    case EventConfirmed(deliveryId, eventPosition) =>
      log.debug(s"[DELIVERY-ID: ${(deliveryId, eventPosition)}] - Delivery confirmed")
      if (confirmDelivery(deliveryId)) {
        state = state.withEventConfirmed(deliveryId, eventPosition)
      }

  }


}
