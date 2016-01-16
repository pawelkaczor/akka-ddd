package pl.newicom.dddd.process

import akka.persistence.RecoveryCompleted
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.alod._
import pl.newicom.dddd.messaging.event.{OfficeEventMessage, EventMessage}

sealed trait SagaAction

case class  RaiseEvent(e: DomainEvent)  extends SagaAction
case object DropEvent                   extends SagaAction
case object RejectEvent                 extends SagaAction

trait SagaAbstractStateHandling {
  type ReceiveEvent = PartialFunction[DomainEvent, SagaAction]

  def receiveEvent: ReceiveEvent

  def updateState(event: DomainEvent): Unit
}


abstract class Saga extends SagaBase {
  this: SagaAbstractStateHandling =>

  override def receiveRecover: Receive = {
    case rc: RecoveryCompleted =>
      // do nothing
    case msg: Any =>
      _updateState(msg)
  }

  override def receiveCommand: Receive = {
    case em @ OfficeEventMessage(caseId, event, id, timestamp, metadata) =>
      val action = receiveEvent(event)
      action match {
        case RaiseEvent(raisedEvent) =>
          // TODO caseId of original OEM will be lost, consider storing it in metadata
          val raisedEventMsg = if (raisedEvent == event) em else EventMessage(raisedEvent)
          persist(raisedEventMsg) { persisted =>
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

  private def _updateState(msg: Any): Unit = {
    msg match {
      case EventMessage(_, receipt: Delivered) =>
        confirmDelivery(receipt.deliveryId)
        updateState(receipt)

      case em: EventMessage =>
        messageProcessed(em)
        updateState(em.event)
    }
  }

  def onEventReceived(em: EventMessage, appliedAction: SagaAction): Unit = {
    appliedAction match {
      case RejectEvent =>
        log.warning(s"Event message rejected: $em")
      case DropEvent =>
        log.debug(s"Event dropped: ${em.event}")
      case RaiseEvent(e) =>
        log.debug(s"Event raised: $e")
    }
  }

}