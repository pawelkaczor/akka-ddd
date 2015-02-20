package pl.newicom.dddd.delivery

import scala.collection.immutable.SortedMap
import scala.util.Try

/**
 * State of delivery (At-Least-Once-Delivery)
 *
 * @param recentlyConfirmed
 *                      recently confirmed deliveryId
 * @param unconfirmed  
 *                    map of unconfirmed (deliveryId -> internal deliveryId)
 */
case class AtLeastOnceDeliveryState(recentlyConfirmed: Option[Long] = None, unconfirmed: SortedMap[Long, Long] = SortedMap.empty[Long, Long]) {

  def internalDeliveryId(deliveryId: Long) = unconfirmed.get(deliveryId)

  def withSent(internalDeliveryId: Long, deliveryId: Long) =
    AtLeastOnceDeliveryState(recentlyConfirmed, unconfirmed.updated(deliveryId, internalDeliveryId))

  def withDelivered(deliveryId: Long) =
    AtLeastOnceDeliveryState(Some(deliveryId), unconfirmed - deliveryId)

  def oldestUnconfirmed: Option[Long] = Try(unconfirmed.lastKey).toOption

}