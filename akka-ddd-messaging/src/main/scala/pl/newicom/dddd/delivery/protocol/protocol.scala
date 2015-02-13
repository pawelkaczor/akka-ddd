package pl.newicom.dddd.delivery.protocol

case class Acknowledged(msg: Any = "OK")

trait Receipt

case object Receipt                                            extends Receipt
case class Confirm(deliveryId: Long)                           extends Receipt
case class ConfirmEvent(deliveryId: Long, eventPosition: Long) extends Receipt
