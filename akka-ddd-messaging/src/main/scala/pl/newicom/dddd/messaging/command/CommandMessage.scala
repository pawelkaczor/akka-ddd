package pl.newicom.dddd.messaging.command

import java.util.Date

import pl.newicom.dddd.aggregate.{Command, EntityId}
import pl.newicom.dddd.messaging.{MetaData, AddressableMessage, Message}
import pl.newicom.dddd.utils.UUIDSupport.uuid

case class CommandMessage(
    command: Command,
    id: String = uuid,
    timestamp: Date = new Date,
    metadata: Option[MetaData] = None)
  extends Message with AddressableMessage {

  type MessageImpl = CommandMessage

  override def destination: Option[EntityId] = Some(command.aggregateId)

  override def payload: Any = command

  override def copyWithMetaData(m: Option[MetaData]): CommandMessage = copy(metadata = m)

  override def toString: String = {
    val msgClass = getClass.getSimpleName
    s"$msgClass(command = $command, id = $id, timestamp = $timestamp, metaData = $metadata)"
  }

}