package pl.newicom.dddd.messaging.command

import java.util.{Date, UUID}

import pl.newicom.dddd.aggregate.{EntityId, Command}
import pl.newicom.dddd.messaging.{EntityMessage, Message}

case class CommandMessage(
    command: Command,
    id: String = UUID.randomUUID().toString,
    timestamp: Date = new Date)
  extends Message with EntityMessage {

  override def entityId: EntityId = command.aggregateId

  override def payload: Any = command
}