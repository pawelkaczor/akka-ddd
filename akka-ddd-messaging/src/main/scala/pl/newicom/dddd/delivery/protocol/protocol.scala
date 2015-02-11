package pl.newicom.dddd.delivery.protocol
import pl.newicom.dddd.aggregate.DomainEvent

case class Acknowledged(msg: Any = "OK")

trait Receipt

case object Receipt                                            extends Receipt
case class ViewUpdated(event: DomainEvent)                     extends Receipt
case class Confirm(deliveryId: Long)                           extends Receipt
case class ConfirmEvent(deliveryId: Long, eventPosition: Long) extends Receipt
