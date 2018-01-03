package pl.newicom.dddd.process

import akka.persistence.Recovery
import pl.newicom.dddd.actor.{Config, ConfigClass}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.LocalOfficeId.fromRemoteId
import pl.newicom.dddd.office.{LocalOfficeId, OfficeId}
import pl.newicom.dddd.process.CommandSink.CommandSinkBehavior

object CommandSink extends AggregateRootSupport {

  def commandSinkLocalOfficeId(department: String): LocalOfficeId[CommandSink] =
    fromRemoteId[CommandSink](commandSinkOfficeId(department))

  sealed trait CommandSinkBehavior extends Behavior[CommandEnqueued, CommandSinkBehavior, Config]

  implicit case object Uninitialized extends CommandSinkBehavior with Uninitialized[CommandSinkBehavior] {
    def actions: Actions =
      handleCommand {
        case EnqueueCommand(command, officeId, department) =>
          CommandEnqueued(command, officeId, department)
      }
      .handleEvent {
        case CommandEnqueued(_, _, _) => this
      }
  }

}

class CommandSink(override val config: Config)(implicit val officeID: LocalOfficeId[CommandSink])
  extends AggregateRoot[CommandEnqueued, CommandSinkBehavior, CommandSink]
    with ConfigClass[Config] {


  override def toEventMessage(event: DomainEvent, source: OfficeId, causedBy: CommandMessage): EventMessage = event match {
    case CommandEnqueued(_, _, department) =>
      super.toEventMessage(event, source, causedBy)
        .withTag(commandQueue(department).streamName)
    case _ =>
      super.toEventMessage(event, source, causedBy)
  }


  // Skip recovery
  override def recovery = Recovery(toSequenceNr = 0L)

  // Disable automated recovery on restart
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = ()

}
