package pl.newicom.dddd.messaging

import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.{Inner, HandledCompletely}
import scala.collection.mutable

/**
  * Designed to be used by persistent actors. Allows detecting duplicated messages sent to the actor.
  * Keeps a set of message IDs that were received by the actor.
  * Additionally keeps an ID of recently received message that is used by Saga to detect out-of-order messages.
  *
  * Provides messageProcessed(Message) method that should be called during the "update-state" stage.
  * The given message must contain [[pl.newicom.dddd.messaging.MetaData.CausationId]] attribute
  * referring to the ID of the received message.
  */
trait Deduplication {
  this: ReceivePipeline =>
  private val ids: mutable.Set[String] = mutable.Set.empty
  private var _idOfRecentlyReceivedMsg: Option[String] = None

  pipelineInner {
    case msg: Message =>
      if (wasReceived(msg)) {
        handleDuplicated(msg)
        HandledCompletely
      } else {
        Inner(msg)
      }
  }

  def handleDuplicated(msg: Message)

  def messageProcessed(msg: Message): Unit =
    msg.causationId.foreach(messageReceived)

  def idOfRecentlyReceivedMessage: Option[String] =
    _idOfRecentlyReceivedMsg

  private def wasReceived(msg: Message): Boolean =
    ids.contains(msg.id)

  private def messageReceived(msgId: String): Unit = {
    ids += msgId
    _idOfRecentlyReceivedMsg = Some(msgId)
  }

}
