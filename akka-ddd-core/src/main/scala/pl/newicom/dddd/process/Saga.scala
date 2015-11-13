package pl.newicom.dddd.process

import akka.persistence.RecoveryCompleted
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.alod._
import pl.newicom.dddd.messaging.event.EventMessage

sealed trait SagaAction

case object ProcessEvent extends SagaAction
case object DropEvent extends SagaAction
case object RejectEvent extends SagaAction

trait SagaState[T <: SagaState[T]]

trait SagaAbstractStateHandling {
  type ReceiveEvent = PartialFunction[DomainEvent, SagaAction]

  def updateState(event: DomainEvent): Unit
  def receiveEvent: ReceiveEvent
}


trait Saga extends SagaBase {
  this: SagaAbstractStateHandling =>

  override def receiveRecover: Receive = {
    case rc: RecoveryCompleted =>
      // do nothing
    case msg: Any =>
      _updateState(msg)
  }

  override def receiveCommand: Receive = {
    case em @ EventMessage(_, event) =>
      val action = receiveEvent.applyOrElse(event, (e: DomainEvent) => RejectEvent)
      action match {
        case ProcessEvent =>
          persist(em) { persisted =>
            log.debug("Event message persisted: {}", persisted)
            _updateState(persisted)
            acknowledgeEvent(persisted)
          }
        case DropEvent =>
          acknowledgeEvent(em)
        case RejectEvent =>
          // rejected event should be redelivered by SagaManager
      }
      onEventReceived(em, action)

    case receipt: Delivered =>
      persist(EventMessage(receipt))(_updateState)
  }

  def receiveEvent: ReceiveEvent

  private def _updateState(msg: Any): Unit = {
    msg match {
      case EventMessage(_, receipt: Delivered) =>
        confirmDelivery(receipt.deliveryId)
        log.debug(s"Delivery of message confirmed (receipt: $receipt)")
        updateState(receipt)
      case em: EventMessage =>
        messageProcessed(em)
        updateState(em.event)
    }
  }

  def onEventReceived(em: EventMessage, appliedAction: SagaAction): Unit = {
    appliedAction match {
      case RejectEvent =>
        log.warning(s"Event rejected: $em")
      case DropEvent =>
        log.debug(s"Event dropped: ${em.event}")
      case ProcessEvent =>
        log.debug(s"Event processed: ${em.event}")
    }
  }

}