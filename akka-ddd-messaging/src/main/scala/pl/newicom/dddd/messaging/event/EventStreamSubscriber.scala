package pl.newicom.dddd.messaging.event

import akka.actor.{Actor, ActorRef}
import pl.newicom.dddd.messaging.MetaData

trait EventStreamSubscriber {
  this: Actor =>

  /**
   * Subscribes this actor to event stream of given name.
   * @param fromPositionExclusive if provided Subscriber will be receiving events
   *                              from given position (exclusively)
   */
  def subscribe(streamName: String, fromPositionExclusive: Option[Long]): ActorRef

  /**
   * Logic of receiving event messages from event stream.
   * Should call [[eventReceived]] with event message enriched with metadata obtained
   * from given metadata provider once the event is received from the stream.
   */
  def receiveEvent(metaDataProvider: EventMessage => Option[MetaData]): Receive

  /**
   * Called whenever event has been received from the stream.
   */
  def eventReceived(em: EventMessage, position: Long): Unit

}
