package pl.newicom.dddd.messaging.event

import akka.actor.Actor.Receive
import akka.actor.ActorRef
import org.json4s.Formats
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._

trait DurableEventstreamSubscriber {

  def formats: Formats

  def streamName: String

  def subscribe: ActorRef

  def receiveEvent: Receive

  def updateState(msg: Any): Unit

  def nextSubscribePosition: Option[Long]

  def customMetadata(em: EventMessage): Option[MetaData]

  def eventPosition(em: EventMessage) = em.getMetaAttribute[Long](EventPosition)

}
