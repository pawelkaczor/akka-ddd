package pl.newicom.dddd.messaging.command

import pl.newicom.dddd.aggregate.{Command, EntityId}
import pl.newicom.dddd.messaging.{AddressableMessage, Message, MetaData}

case class CommandMessage(command: Command, metadata: MetaData = MetaData.initial)
    extends Message
    with AddressableMessage {

  type MessageImpl = CommandMessage

  override def destination: Option[EntityId] =
    Some(command.aggregateId)

  override def payload: Any = command

  override protected def withNewMetaData(m: MetaData): CommandMessage =
    copy(metadata = m)

  override def toString: String = {
    val msgClass = getClass.getSimpleName
    s"$msgClass(command = $command, metaData = $metadata)"
  }

}
