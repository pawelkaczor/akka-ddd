package pl.newicom.dddd.delivery

import pl.newicom.dddd.aggregate.EntityId

import scala.collection.immutable.SortedMap

case class UnconfirmedMessageEntry(internalDeliveryId: Long, destinationId: String, msgId: String)

/**
 * State of delivery (At-Least-Once-Delivery)
 *
 */
sealed trait DeliveryState {

  /**
   * @param deliveryId delivery id
   * @return <b>unconfirmed</b> internal deliveryId
   */
  def internalDeliveryId(deliveryId: Long): Option[Long]

  def withDelivered(deliveryId: Long): DeliveryState

  def withSent(msgId: String, internalDeliveryId: Long, deliveryId: Long, destinationId: EntityId): DeliveryState

  /**
   * @return last sent deliveryId
   */
  def lastSentOpt: Option[Long]

  def unconfirmedNumber: Int

  def lastSentToDestinationMsgId(destinationId: EntityId): Option[EntityId]

}

case object InitialState extends DeliveryState {

  def withSent(msgId: String, internalDeliveryId: Long, deliveryId: Long, destinationId: EntityId) =
    new DeliveryInProgressState(
      lastSent = deliveryId,
      size = 1,
      unconfirmed = SortedMap(deliveryId -> UnconfirmedMessageEntry(internalDeliveryId, destinationId, msgId)),
      lastSentMsgIdPerDestination = Map(destinationId -> msgId))

  def withDelivered(deliveryId: Long) = this

  def internalDeliveryId(deliveryId: Long) = None

  def lastSentOpt = None

  def lastSentToDestinationMsgId(destinationId: EntityId) = None

  def unconfirmedNumber = 0

}

case class DeliveryInProgressState(lastSent: Long, size: Int, unconfirmed: SortedMap[Long, UnconfirmedMessageEntry], lastSentMsgIdPerDestination: Map[EntityId, String]) extends DeliveryState {

  def unconfirmedEntry(deliveryId: Long): Option[UnconfirmedMessageEntry] = unconfirmed.get(deliveryId)

  def internalDeliveryId(deliveryId: Long): Option[Long] =
    unconfirmedEntry(deliveryId).map(_.internalDeliveryId)

  def withSent(msgId: String, internalDeliveryId: Long, deliveryId: Long, destinationId: EntityId) =
    new DeliveryInProgressState(
      lastSent = deliveryId,
      size = size + 1,
      unconfirmed = unconfirmed.updated(deliveryId, UnconfirmedMessageEntry(internalDeliveryId, destinationId, msgId)),
      lastSentMsgIdPerDestination = lastSentMsgIdPerDestination.updated(destinationId, msgId)
    )

  def withDelivered(deliveryId: Long): DeliveryState =
    unconfirmedEntry(deliveryId).map { entry =>
      copy(
        size                        = size - 1,
        unconfirmed                 = unconfirmed - deliveryId,
        lastSentMsgIdPerDestination = lastSentMsgIdPerDestination.dropWhile({
                    case (destinationId, msgId) =>
                      entry.destinationId == destinationId && entry.msgId == msgId
                    })
      )
    }.getOrElse(this)

  def lastSentOpt: Some[Long] =
    Some(lastSent)

  def unconfirmedNumber: Int =
    size

  def lastSentToDestinationMsgId(destinationId: EntityId): Option[String] =
    lastSentMsgIdPerDestination.get(destinationId)
}