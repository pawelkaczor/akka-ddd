package pl.newicom.dddd.delivery

import scala.collection.immutable.SortedMap

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

  def withSent(internalDeliveryId: Long, deliveryId: Long): DeliveryState

  /**
   * @return last sent deliveryId
   */
  def lastSentOpt: Option[Long]

  def unconfirmedNumber: Int

}

case object InitialState extends DeliveryState {

  def withSent(internalDeliveryId: Long, deliveryId: Long) =
    new DeliveryInProgressState(internalDeliveryId, deliveryId)

  def withDelivered(deliveryId: Long) = this

  def internalDeliveryId(deliveryId: Long) = None

  def lastSentOpt = None

  def unconfirmedNumber = 0

}

case class DeliveryInProgressState(lastSent: Long, size: Int, unconfirmed: SortedMap[Long, Long]) extends DeliveryState {

  def this(internalDeliveryId: Long, deliveryId: Long) =
    this(deliveryId, 1, SortedMap(deliveryId -> internalDeliveryId))

  def internalDeliveryId(deliveryId: Long) =
    unconfirmed.get(deliveryId)

  def withSent(internalDeliveryId: Long, deliveryId: Long) =
    new DeliveryInProgressState(deliveryId, size + 1, unconfirmed.updated(deliveryId, internalDeliveryId))

  def withDelivered(deliveryId: Long) =
    copy(size = size - 1, unconfirmed = unconfirmed - deliveryId)

  def lastSentOpt = Some(lastSent)

  def unconfirmedNumber = size

}