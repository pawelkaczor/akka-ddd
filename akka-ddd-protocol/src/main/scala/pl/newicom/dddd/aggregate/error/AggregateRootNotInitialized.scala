package pl.newicom.dddd.aggregate.error

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.aggregate.error.AggregateRootNotInitialized._

object AggregateRootNotInitialized {
  def msg(caseName: String, id: EntityId, commandName: String, eventName: Option[String]): String =
    if (eventName.isDefined) {
      s"$caseName with ID $id does not exist. $commandName can not be processed: missing state initialization for event: ${eventName.get}!"
    } else {
      s"$caseName with ID $id does not exist. $commandName can not be processed: missing command handler!"
    }
}

class AggregateRootNotInitialized(caseName: String, id: EntityId, commandName: String, eventName: Option[String] = None) extends CommandRejected(msg(caseName, id, commandName, eventName))