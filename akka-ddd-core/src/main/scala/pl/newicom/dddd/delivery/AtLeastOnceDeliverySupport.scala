package pl.newicom.dddd.delivery

import akka.actor.ActorPath
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import akka.persistence._
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.alod.Delivered
import pl.newicom.dddd.messaging.{AddressableMessage, Message}
import pl.newicom.dddd.persistence.{PersistentActorLogging, SaveSnapshotRequest}

case class DeliveryStateSnapshot(state: DeliveryState, alodSnapshot: AtLeastOnceDeliverySnapshot)

trait AtLeastOnceDeliverySupport extends PersistentActor with AtLeastOnceDelivery with PersistentActorLogging {

  type DeliverableMessage = Message with AddressableMessage

  def isSupporting_MustFollow_Attribute: Boolean = true

  private var deliveryState: DeliveryState = InitialState

  def destination(msg: Message): ActorPath

  def recoveryCompleted(): Unit

  def lastSentDeliveryId: Option[Long]          = deliveryState.lastSentOpt
  def oldestUnconfirmedDeliveryId: Option[Long] = deliveryState.oldestUnconfirmedDeliveryId
  def unconfirmedNumber: Int                    = deliveryState.unconfirmedNumber

  def deliver(msg: Message, deliveryId: Long): Unit =
    persist(msg.withDeliveryId(deliveryId))(updateState)

  def deliveryIdToMessage(msg: DeliverableMessage, destination: ActorPath): Long ⇒ Any = internalDeliveryId => {
    val deliveryId                                   = msg.deliveryId.get
    val destinationId: EntityId                      = msg.destination.get
    val lastSentToDestinationMsgId: Option[EntityId] = deliveryState.lastSentToDestinationMsgId(destinationId)
    deliveryState = deliveryState.withSent(msg.id, internalDeliveryId, deliveryId, destinationId)

    val msgToDeliver =
      if (isSupporting_MustFollow_Attribute) msg.withMustFollow(lastSentToDestinationMsgId)
      else msg

    log.debug(s"[DELIVERY-ID: $deliveryId] Delivering: $msgToDeliver to $destination")
    msgToDeliver
  }

  def updateState(msg: Any): Unit = msg match {
    case message: DeliverableMessage =>
      if (message.destination.isEmpty) {
        log.warning(s"No entityId. Skipping $message")
      } else {
        val dest: ActorPath = destination(message)
        deliver(dest)(deliveryIdToMessage(message, dest))
      }

    case receipt: Delivered =>
      val deliveryId = receipt.deliveryId
      deliveryState.internalDeliveryId(deliveryId).foreach { internalDeliveryId =>
        log.debug(s"[DELIVERY-ID: $deliveryId] - Delivery confirmed")
        if (confirmDelivery(internalDeliveryId)) {
          deliveryState = deliveryState.withDelivered(deliveryId)
          deliveryConfirmed(internalDeliveryId)
        }
      }

  }

  def deliveryStateReceive: Receive = {
    case receipt: Delivered =>
      persist(receipt)(updateState)

    case SaveSnapshotRequest =>
      val snapshot = DeliveryStateSnapshot(deliveryState, getDeliverySnapshot)
      saveSnapshot(snapshot)

    case SaveSnapshotSuccess(metadata) =>
      log.debug("Snapshot saved successfully with metadata: {}", metadata)

    case f @ SaveSnapshotFailure(metadata, reason) =>
      log.error(s"$f")
      throw reason

  }

  override def receiveRecover: Receive = {
    case RecoveryCompleted =>
      log.debug("Recovery completed")
      recoveryCompleted()

    case SnapshotOffer(metadata, DeliveryStateSnapshot(dState, alodSnapshot)) =>
      setDeliverySnapshot(alodSnapshot)
      deliveryState = dState
      log.debug(s"Snapshot restored: $deliveryState")

    case msg =>
      updateState(msg)
  }

  def deliveryConfirmed(deliveryId: Long): Unit = {
    // do nothing
  }
}
