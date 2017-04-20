package pl.newicom.dddd.aggregate.error
import StateTransitionNotDefined._

object StateTransitionNotDefined {
  def msg(commandName: String, eventName: String) =
    s"$commandName can not be processed. State transition not defined for event: $eventName!"
}


class StateTransitionNotDefined(commandName: String, eventName: String) extends CommandRejected(msg(commandName, eventName))
