package pl.newicom.dddd.process

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, GracefulPassivation, PassivationConfig}
import pl.newicom.dddd.aggregate.{BusinessEntity, DomainEvent}
import pl.newicom.dddd.delivery.protocol.ConfirmEvent
import pl.newicom.dddd.messaging.MetaData.{DeliveryId, EventPosition}
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.messaging.{Deduplication, Message}

abstract class SagaActorFactory[A <: Saga] extends BusinessEntityActorFactory[A] {
  import scala.concurrent.duration._

  def props(pc: PassivationConfig): Props
  def inactivityTimeout: Duration = 1.minute
}

trait Saga extends BusinessEntity with GracefulPassivation with PersistentActor with Deduplication with ActorLogging {

  def sagaId = self.path.name

  override def id = sagaId

  override def persistenceId: String = sagaId

  override def aroundReceive(receive: Receive, msg: Any): Unit = {
    super.aroundReceive(receiveDuplicate(acknowledge).orElse(receive), msg)
  }

  override def receiveCommand: Receive = receiveEvent.orElse(receiveUnexpected)

  /**
   * Defines business process logic (state transitions).
   * State transition happens when raise(event) is called.
   * No state transition indicates the current event message could have been received out-of-order.
   */
  def receiveEvent: Receive

  override def receiveRecover: Receive = {
    case em: EventMessage =>
      updateState(em)
  }

  /**
   * Triggers state transition
   */
  def raise(em: EventMessage): Unit =
    persist(em) { persisted =>
      log.debug("Event message persisted: {}", persisted)
      updateState(persisted)
      acknowledge(persisted)
    }

  /**
   * Event handler called on state transition
   */
  def updateState(e: DomainEvent)

  private def updateState(em: EventMessage) {
    eventProcessed(em)
    updateState(em.event)
  }

  private def acknowledge(m: Message) {
    val confirmation = ConfirmEvent(m.getMetaAttribute(DeliveryId), m.getMetaAttribute(EventPosition))
    sender() ! confirmation
    log.debug(s"Confirmation $confirmation sent")
  }

  def receiveUnexpected: Receive = {
    case em: EventMessage => handleUnexpectedEvent(em)
  }

  def handleUnexpectedEvent(em: EventMessage): Unit = {
    log.warning(s"Unhandled: $em") // unhandled event should be redelivered by SagaManager
  }

}

