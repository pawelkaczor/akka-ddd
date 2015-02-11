package pl.newicom.dddd.messaging

import akka.actor.Actor.Receive

import scala.collection.mutable

trait Deduplication {

  private val processedEvents: mutable.Set[String] = mutable.Set.empty

  def receiveDuplicate(handleDuplicate: Message => Unit): Receive = {
    case m: Message if wasProcessed(m) =>
      handleDuplicate(m)
  }

  def eventProcessed(m: Message): Unit = {
    processedEvents += m.id
  }

  def wasProcessed(m: Message): Boolean =
    processedEvents.contains(m.id)

}
