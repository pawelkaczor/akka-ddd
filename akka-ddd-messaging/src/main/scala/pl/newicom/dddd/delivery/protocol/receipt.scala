package pl.newicom.dddd.delivery.protocol

trait Receipt

case class Acknowledged(msg: Any = "OK")                       extends Receipt
case class Confirm(deliveryId: Long)                           extends Receipt
case class ConfirmEvent(deliveryId: Long, eventPosition: Long) extends Receipt