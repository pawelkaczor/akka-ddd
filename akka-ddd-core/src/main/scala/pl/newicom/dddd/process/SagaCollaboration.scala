package pl.newicom.dddd.process

import akka.actor.{ActorPath, ActorSystem}
import org.joda.time.DateTime.now
import org.joda.time.{DateTime, Period}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.DeliveryHandler
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.{Message, MetaData, MetaDataPropagationPolicy}
import pl.newicom.dddd.office._
import pl.newicom.dddd.scheduling.ScheduleEvent
import pl.newicom.dddd.utils.UUIDSupport.uuid

trait SagaCollaboration {
  this: SagaBase =>

  protected def deliverMsg(target: ActorPath, msg: Message): Unit = {
    deliver(target)(msg.withDeliveryId(_))
  }

  protected def deliverCommand(target: ActorPath, command: Command): Unit = {
    val metadata = MetaDataPropagationPolicy.onCommandSentByPM(currentEventMsg.metadata)
    deliverMsg(target, CommandMessage(command, metadata))
  }

  protected def schedule(event: DomainEvent, deadline: DateTime, correlationId: EntityId = sagaId): Unit = {
    val command = ScheduleEvent("global", officePath, deadline, event)
    handlerOf(command) !! {
      CommandMessage(command, MetaData.initial(uuid(currentEventMsg.id)))
        .withCorrelationId(correlationId)
        .withTag(officeId.id)
    }
  }


  //
  // DSL helpers
  //

  def ⟶[C <: Command](command: C): Unit = {
    val dc = deliverableCommand(command)
    handlerOf(dc) !! dc
  }

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


  //
  // Private members
  //

  private implicit lazy val as: ActorSystem = context.system

  private lazy val commandHandlerResolver = new CommandHandlerResolver()
  private lazy val officeRegistry = OfficeRegistry(as)

  private def deliverableCommand(command: Command): Command = {
    val officeId = commandHandlerResolver(command)
    if (officeRegistry.isOfficeAvailableInCluster(officeId.id)) {
      command
    } else {
      EnqueueCommand(command, officeId.id, officeId.department)
    }
  }

  private def handlerOf(command: Command): CommandHandler =
    officeRegistry.officeRef(commandHandlerResolver(command).id)

}
