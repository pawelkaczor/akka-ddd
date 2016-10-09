package pl.newicom.dddd.process

import akka.actor.ActorPath
import akka.contrib.pattern.ReceivePipeline
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import org.joda.time.DateTime.now
import org.joda.time.{DateTime, Period}
import pl.newicom.dddd.actor.GracefulPassivation
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.DeliveryHandler
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.messaging.{Deduplication, Message}
import pl.newicom.dddd.office.OfficeFactory.office
import pl.newicom.dddd.office.{Office, OfficeId, RemoteOfficeId}
import pl.newicom.dddd.persistence.PersistentActorLogging
import pl.newicom.dddd.scheduling.ScheduleEvent

import scala.reflect.ClassTag

trait SagaBase extends BusinessEntity with GracefulPassivation with PersistentActor
  with AtLeastOnceDelivery with ReceivePipeline with Deduplication with PersistentActorLogging {

  private var _lastEventMessage: Option[EventMessage] = None

  def sagaId: String = self.path.name

  def officeId: OfficeId

  override def persistenceId: String = officeId.clerkGlobalId(id)

  override def id: EntityId = sagaId
  override def department: String = officeId.department

  def currentEventMsg: EventMessage = _lastEventMessage.get

  def schedulingOffice: Option[Office] = None

  def officePath: ActorPath = context.parent.path.parent

  def deliverMsg(office: ActorPath, msg: Message): Unit = {
    deliver(office)(deliveryId => {
      msg.withMetaAttribute(DeliveryId, deliveryId)
    })
  }


  def deliverCommand(office: ActorPath, command: Command): Unit = {
    deliverMsg(office, CommandMessage(command).causedBy(currentEventMsg))
  }

  def schedule(event: DomainEvent, deadline: DateTime, correlationId: EntityId = sagaId): Unit = {
    schedulingOffice.fold(throw new UnsupportedOperationException("Scheduling Office is not defined.")) { schOffice =>
      val command = ScheduleEvent("global", officePath, deadline, event)
      schOffice deliver CommandMessage(command).withCorrelationId(correlationId)
    }
  }

  protected def acknowledgeEvent(em: Message) {
    val deliveryReceipt = em.deliveryReceipt()
    sender() ! deliveryReceipt
    log.debug(s"Delivery receipt (for received event) sent ($deliveryReceipt)")
  }

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


  //
  // DSL helpers
  //

  def ⟶[C >: Command : ClassTag: RemoteOfficeId](command: C): Unit =
    office(implicitly[RemoteOfficeId[C]])(context.system) deliver command

  def ⟵(event: DomainEvent): ToBeScheduled = schedule(event)

  implicit def deliveryHandler: DeliveryHandler = {
    (ap: ActorPath, msg: Any) => msg match {
      case c: Command => deliverCommand(ap, c)
      case m: Message => deliverMsg(ap, m)
    }
  }.tupled


  def schedule(event: DomainEvent) = new ToBeScheduled(event)

  class ToBeScheduled(event: DomainEvent) {
    def on(dateTime: DateTime): Unit = schedule(event, dateTime)
    def at(dateTime: DateTime): Unit = on(dateTime)
    def in(period: Period): Unit = on(now.plus(period))
    def asap(): Unit = on(now)
  }
}
