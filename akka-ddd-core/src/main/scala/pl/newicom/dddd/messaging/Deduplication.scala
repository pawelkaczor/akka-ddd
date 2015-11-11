package pl.newicom.dddd.messaging

import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.{Inner, HandledCompletely}

import scala.collection.mutable

trait Deduplication {
  this: ReceivePipeline =>

  private val processedMessages: mutable.Set[String] = mutable.Set.empty

  pipelineInner {
    case m: Message =>
      if (wasProcessed(m)) {
        handleDuplicated(m)
        HandledCompletely
      } else {
        Inner(m)
      }
  }

  def handleDuplicated(m: Message)

  def messageProcessed(m: Message): Unit = {
    processedMessages += m.id
  }

  def wasProcessed(m: Message): Boolean =
    processedMessages.contains(m.id)

}
