package pl.newicom.dddd.aggregate.error

import pl.newicom.dddd.aggregate.error.CommandHandlerNotDefined._

object CommandHandlerNotDefined {
  def msg(commandName: String) = s"$commandName can not be processed: missing command handler!"
}

case class CommandHandlerNotDefined(commandName: String) extends CommandRejected(msg(commandName))
