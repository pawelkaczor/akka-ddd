package pl.newicom.dddd.messaging

import akka.actor.Actor.Receive

import scala.collection.mutable

trait Deduplication {

  private val processedMessages: mutable.Set[String] = mutable.Set.empty

  def receiveDuplicate(handleDuplicate: Message => Unit): Receive = {
    case m: Message if wasProcessed(m) =>
      handleDuplicate(m)
  }

  def messageProcessed(m: Message): Unit = {
    processedMessages += m.id
  }

  def wasProcessed(m: Message): Boolean =
    processedMessages.contains(m.id)

}
