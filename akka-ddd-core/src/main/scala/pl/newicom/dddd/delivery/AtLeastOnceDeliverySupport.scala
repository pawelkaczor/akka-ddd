package pl.newicom.dddd.delivery

import akka.actor.{ActorPath, ActorLogging}
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import akka.persistence._
import pl.newicom.dddd.delivery.protocol.alod.Delivered
import pl.newicom.dddd.messaging.{EntityMessage, Message}
import pl.newicom.dddd.persistence.SaveSnapshotRequest

case class DeliveryStateSnapshot(state: DeliveryState, alodSnapshot: AtLeastOnceDeliverySnapshot)

trait AtLeastOnceDeliverySupport extends PersistentActor with AtLeastOnceDelivery with ActorLogging {

  private var deliveryState: DeliveryState = InitialState

  def destination(msg: Message): ActorPath

  def recoveryCompleted(): Unit
  
  def lastSentDeliveryId: Option[Long] = deliveryState.lastSentOpt

  def deliver(msg: Message, deliveryId: Long): Unit =
    persist(msg.withDeliveryId(deliveryId))(updateState)

  def deliveryIdToMessage(msg: Message): Long ⇒ Any = { internalDeliveryId => {
      val deliveryId = msg.deliveryId.get
      log.debug(s"[DELIVERY-ID: $deliveryId] Delivering: $msg")
      deliveryState = deliveryState.withSent(internalDeliveryId, deliveryId)
      msg
    }
  }

  def updateState(msg: Any): Unit = msg match {
    case em: EntityMessage =>
      if (em.entityId == null) {
        log.warning(s"No entityId. Skipping $em")
      } else {
        val message = em.asInstanceOf[Message]
        deliver(destination(message))(deliveryIdToMessage(message))
      }

    case receipt: Delivered =>
      val deliveryId = receipt.deliveryId
      deliveryState.internalDeliveryId(deliveryId).foreach { internalDeliveryId =>
        log.debug(s"[DELIVERY-ID: $internalDeliveryId] - Delivery confirmed")
        if (confirmDelivery(internalDeliveryId)) {
          deliveryState = deliveryState.withDelivered(deliveryId)
        }
      }

  }

  def deliveryStateReceive: Receive = {
    case receipt: Delivered =>
      persist(receipt)(updateState)

    case SaveSnapshotRequest =>
      val snapshot = new DeliveryStateSnapshot(deliveryState, getDeliverySnapshot)
      log.debug(s"Saving snapshot: $snapshot")
      saveSnapshot(snapshot)

    case SaveSnapshotSuccess(metadata) =>
      log.debug("Snapshot saved successfully")

    case f @ SaveSnapshotFailure(metadata, reason) =>
      log.error(s"$f")
      throw reason

  }

  override def receiveRecover: Receive = {
    case RecoveryCompleted  =>
      log.debug("Recovery completed")
      recoveryCompleted()

    case SnapshotOffer(metadata, DeliveryStateSnapshot(dState, alodSnapshot)) =>
      setDeliverySnapshot(alodSnapshot)
      deliveryState = dState
      log.debug(s"Snapshot restored: $deliveryState")

    case msg =>
      updateState(msg)
  }

}
