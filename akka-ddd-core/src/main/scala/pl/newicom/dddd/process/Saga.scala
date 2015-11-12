package pl.newicom.dddd.process

import akka.actor.{ActorLogging, ActorPath, Props}
import akka.contrib.pattern.ReceivePipeline
import akka.persistence.{AtLeastOnceDelivery, PersistentActor, RecoveryCompleted}
import org.joda.time.DateTime
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, GracefulPassivation, PassivationConfig}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.alod._
import pl.newicom.dddd.messaging.MetaData.DeliveryId
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.messaging.{Deduplication, Message}
import pl.newicom.dddd.office.OfficeInfo
import pl.newicom.dddd.scheduling.ScheduleEvent

abstract class SagaActorFactory[A <: Saga] extends BusinessEntityActorFactory[A] {
  import scala.concurrent.duration._

  def props(pc: PassivationConfig): Props
  def inactivityTimeout: Duration = 1.minute
}

/**
 * @param bpsName name of Business Process Stream (bps)
 */
abstract class SagaConfig[A <: Saga](val bpsName: String) extends OfficeInfo[A] {

  def name = bpsName

  /**
   * Correlation ID identifies process instance. It is used to route EventMessage
   * messages created by [[SagaManager]] to [[Saga]] instance,
   */
  def correlationIdResolver: PartialFunction[DomainEvent, EntityId]

}

sealed trait SagaAction

case object ProcessEvent extends SagaAction
case object DropEvent extends SagaAction
case object RejectEvent extends SagaAction

trait Saga extends BusinessEntity with GracefulPassivation with PersistentActor
  with AtLeastOnceDelivery with ReceivePipeline with Deduplication with ActorLogging {

  def sagaId = self.path.name

  override def id = sagaId

  override def persistenceId: String = sagaId

  def schedulingOffice: Option[ActorPath] = None

  def sagaOffice: ActorPath = context.parent.path.parent

  private var _lastEventMessage: Option[EventMessage] = None

  /**
   * Event message being processed.
   */
  def eventMessage = _lastEventMessage.get

  override def receiveCommand: Receive = {
    case em @ EventMessage(_, event) =>
      val action = receiveEvent.applyOrElse(event, (e: DomainEvent) => RejectEvent)
      action match {
        case ProcessEvent => raise(em)
        case DropEvent => acknowledgeEvent(em)
        case RejectEvent => // unhandled event should be redelivered by SagaManager
      }
      actionApplied(em, action)

    case receipt: Delivered =>
      persist(EventMessage(receipt))(updateState)
  }

  def actionApplied(em: EventMessage, sagaAction: SagaAction): Unit = {
    sagaAction match {
      case RejectEvent =>
        log.warning(s"Event rejected: $em")
      case DropEvent =>
        log.debug(s"Event dropped: ${em.event}")
      case ProcessEvent =>
        log.debug(s"Event processed: ${em.event}")
    }
  }

  def deliverMsg(office: ActorPath, msg: Message): Unit = {
    deliver(office)(deliveryId => {
      msg.withMetaAttribute(DeliveryId, deliveryId)
    })
  }

  def deliverCommand(office: ActorPath, command: Command): Unit = {
    deliverMsg(office, CommandMessage(command).causedBy(eventMessage))
  }

  def schedule(event: DomainEvent, deadline: DateTime, correlationId: EntityId = sagaId): Unit = {
    schedulingOffice.fold(throw new UnsupportedOperationException("Scheduling Office is not defined.")) { schOffice =>
      val command = ScheduleEvent("global", sagaOffice, deadline, event)
      deliverMsg(schOffice, CommandMessage(command).withCorrelationId(correlationId))
    }
  }


  /**
   * Defines business process logic (state transitions).
   * State transition happens when SagaAction.ProcessEvent is returned.
   * No state transition indicates the current event message could have been received out-of-order.
   */
  def receiveEvent: PartialFunction[DomainEvent, SagaAction]

  override def receiveRecover: Receive = {
    case rc: RecoveryCompleted =>
      // do nothing
    case msg: Any =>
      updateState(msg)
  }

  /**
   * Triggers state transition
   */
  def raise(em: EventMessage): Unit =
    persist(em) { persisted =>
      log.debug("Event message persisted: {}", persisted)
      updateState(persisted)
      acknowledgeEvent(persisted)
    }

  /**
   * Event handler called on state transition
   */
  def applyEvent: PartialFunction[DomainEvent, Unit]

  private def updateState(msg: Any): Unit = msg match {
    case EventMessage(_, receipt: Delivered) =>
      confirmDelivery(receipt.deliveryId)
      log.debug(s"Delivery of message confirmed (receipt: $receipt)")
      applyEvent.applyOrElse(receipt, (e: DomainEvent) => ())
    case em: EventMessage =>
      messageProcessed(em)
      applyEvent.applyOrElse(em.event, (e: DomainEvent) => ())
  }

  private def acknowledgeEvent(em: Message) {
    val deliveryReceipt = em.deliveryReceipt()
    sender() ! deliveryReceipt
    log.debug(s"Delivery receipt (for received event) sent ($deliveryReceipt)")
  }

  def handleDuplicated(m: Message) =
    acknowledgeEvent(m)

  override def messageProcessed(m: Message): Unit = {
    _lastEventMessage = m match {
      case em: EventMessage =>
        Some(em)
      case _ => None
    }
    super.messageProcessed(m)
  }
}