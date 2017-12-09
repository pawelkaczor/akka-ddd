package pl.newicom.dddd.process

import akka.actor.{ActorPath, ActorSystem}
import org.joda.time.DateTime.now
import org.joda.time.{DateTime, Period}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.DeliveryHandler
import pl.newicom.dddd.messaging.{Message, MetaData, MetaDataPropagationPolicy}
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.office._
import pl.newicom.dddd.scheduling.ScheduleEvent

trait SagaCollaboration {
  this: SagaBase =>

  protected def deliverMsg(target: ActorPath, msg: Message): Unit = {
    deliver(target)(msg.withDeliveryId(_))
  }

  protected def deliverCommand(target: ActorPath, command: Command): Unit = {
    deliverMsg(target, CommandMessage(command).withMetaData(
      MetaDataPropagationPolicy.onCommandSentByPM(currentEventMsg.metadata, MetaData.empty))
    )
  }

  protected def schedule(event: DomainEvent, deadline: DateTime, correlationId: EntityId = sagaId): Unit = {
    val command = ScheduleEvent("global", officePath, deadline, event)
    handlerOf(command) !! {
      CommandMessage(command)
        .withCorrelationId(correlationId)
        .withTag(officeId.id)
    }
  }


  //
  // DSL helpers
  //

  def ⟶[C <: Command](command: C): Unit = {
    handlerOf(command) !! command
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

  private implicit val as: ActorSystem = context.system

  private val officeIdResolver = new CommandHandlerResolver
  private val officeRegistry = OfficeRegistry(as)

  private def handlerOf(command: Command): CommandHandler =
    officeRegistry.officeRef(officeIdResolver(command).id)

}
