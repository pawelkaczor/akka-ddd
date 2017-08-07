package pl.newicom.dddd.process

import akka.persistence.RecoveryCompleted
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.alod._
import pl.newicom.dddd.messaging.MetaAttribute.{Publisher_Type, Reused}
import pl.newicom.dddd.messaging.PublisherTypeValue.BP
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.messaging.{MetaData, MetaDataPropagationPolicy}

case object EventDroppedMarkerEvent extends DomainEvent

sealed trait SagaAction

case class  RaiseEvent(e: DomainEvent)  extends SagaAction
case object DropEvent                   extends SagaAction

trait SagaAbstractStateHandling {
  type ReceiveEvent = PartialFunction[DomainEvent, SagaAction]

  def receiveEvent: ReceiveEvent

  def updateState(event: DomainEvent): Unit

  def initialized: Boolean
}


abstract class Saga extends SagaBase {
  this: SagaAbstractStateHandling =>

  override def receiveRecover: Receive = {
    case _: RecoveryCompleted =>
      // do nothing
    case msg: Any =>
      _updateState(msg)
  }

  override def receiveCommand: Receive = {
    case em: EventMessage =>
      val event = em.event
	    val actionMaybe: Option[SagaAction] =
        em.mustFollow.fold(Option(receiveEvent(event))) { mustFollow =>
          if (wasReceived(mustFollow))
            Option(receiveEvent(event))
          else
            None
        }

      if (actionMaybe.isEmpty) {
        log.debug("Message out of order detected: {}", em.id)
      } else {
        val action  = actionMaybe.get
        val eventToPersist = action match {
          case RaiseEvent(raisedEvent) => raisedEvent
          case DropEvent => EventDroppedMarkerEvent
        }

        val emToPersist = EventMessage(eventToPersist).withMetaData(
          MetaDataPropagationPolicy.onEventAcceptedByPM(
            receivedAttrs = em.metadata,
            targetAttrs   = MetaData(Publisher_Type -> BP, Reused -> (eventToPersist == event))
          ))

        persist(emToPersist) { persisted =>
          log.debug("Event message persisted: {}", persisted)
          _updateState(persisted)
          acknowledgeEvent(persisted)
        }

        onEventReceived(em, action)
      }

    case receipt: Delivered if initialized =>
      persist(EventMessage(receipt).withPublisherType(BP))(_updateState)
  }

  private def _updateState(msg: Any): Unit = {
    msg match {
      case EventMessage(_, receipt: Delivered) =>
        confirmDelivery(receipt.deliveryId)
        updateState(receipt)

      case em: EventMessage => em.event match {
        case EventDroppedMarkerEvent =>
          messageProcessed(em)
        case event =>
          messageProcessed(em)
          updateState(event)
      }
    }
  }

  def onEventReceived(em: EventMessage, appliedAction: SagaAction): Unit = {
    appliedAction match {
      case DropEvent =>
        log.debug(s"Event dropped: ${em.event}")
      case RaiseEvent(e) =>
        log.debug(s"Event raised: $e")
    }
  }

}