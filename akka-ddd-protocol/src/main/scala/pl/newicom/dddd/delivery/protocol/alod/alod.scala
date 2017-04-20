package pl.newicom.dddd.delivery.protocol.alod

import pl.newicom.dddd.delivery.protocol.Receipt

import scala.util.{Success, Try}

/**
  * At-Least-Once Delivery protocol
  */
object Delivered {
  val CommandAccepted = Success("Command processed successfully. Thank you!")
}

trait Delivered extends Receipt {
  def deliveryId: Long
}

case class Received(deliveryId: Long)                                    extends Delivered
case class Processed(deliveryId: Long, result: Try[Any] = Success("OK")) extends Delivered
