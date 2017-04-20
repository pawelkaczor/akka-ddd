package pl.newicom.dddd.aggregate.error

import CommandHandlerNotDefined._

object CommandHandlerNotDefined {
  def msg(commandName: String) = s"$commandName can not be processed: missing command handler!"
}

class CommandHandlerNotDefined(commandName: String) extends CommandRejected(msg(commandName))
