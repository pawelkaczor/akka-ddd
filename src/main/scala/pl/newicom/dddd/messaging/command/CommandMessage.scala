package pl.newicom.dddd.messaging.command

import java.util.{Date, UUID}

import pl.newicom.dddd.aggregate.BusinessEntity.EntityId
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.messaging.{EntityMessage, Message}

case class CommandMessage(
    command: Command,
    identifier: String = UUID.randomUUID().toString,
    timestamp: Date = new Date)
  extends Message with EntityMessage {

  override def entityId: EntityId = command.aggregateId

  override def payload: Any = command
}