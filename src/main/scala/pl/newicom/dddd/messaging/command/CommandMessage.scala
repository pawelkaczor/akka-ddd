package pl.newicom.dddd.messaging.command

import java.util.{Date, UUID}
import pl.newicom.dddd.aggregate.{Command, BusinessEntity}
import BusinessEntity.EntityId
import pl.newicom.dddd.messaging.{EntityMessage, Message}
import Message.MetaData

case class CommandMessage(
    command: Command,
    identifier: String = UUID.randomUUID().toString,
    timestamp: Date = new Date,
    metaData: Option[MetaData] = None)
  extends Message(metaData) with EntityMessage {

  override def entityId: EntityId = command.aggregateId

  override def payload: Any = command
}