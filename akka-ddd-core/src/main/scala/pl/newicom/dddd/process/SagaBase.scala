package pl.newicom.dddd.process

import akka.actor.ActorPath
import akka.contrib.pattern.ReceivePipeline
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import pl.newicom.dddd.actor.GracefulPassivation
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.messaging.{Deduplication, Message}
import pl.newicom.dddd.office.OfficeId
import pl.newicom.dddd.persistence.PersistentActorLogging

trait SagaBase extends BusinessEntity with GracefulPassivation with PersistentActor
  with AtLeastOnceDelivery with ReceivePipeline with Deduplication with PersistentActorLogging {

  private var _lastEventMessage: Option[EventMessage] = None

  override def persistenceId: String =
    officeId.clerkGlobalId(id)

  override def id: EntityId =
    sagaId

  override def department: String =
    officeId.department

  override def messageProcessed(msg: Message): Unit = {
    _lastEventMessage = msg match {
      case em: EventMessage =>
        Some(em)
      case _ => None
    }
    super.messageProcessed(msg)
  }

  override def handleDuplicated(msg: Message): Unit =
    acknowledgeEvent(msg)

  protected def sagaId: String = self.path.name

  protected def officeId: OfficeId

  protected def currentEventMsg: EventMessage =
    _lastEventMessage.get

  protected def acknowledgeEvent(em: Message) {
    val deliveryReceipt = em.deliveryReceipt()
    sender() ! deliveryReceipt
    log.debug(s"Delivery receipt (for received event) sent ($deliveryReceipt)")
  }

  protected def officePath: ActorPath = context.parent.path.parent

}
