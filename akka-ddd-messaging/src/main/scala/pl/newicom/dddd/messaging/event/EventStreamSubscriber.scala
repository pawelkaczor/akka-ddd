package pl.newicom.dddd.messaging.event

import akka.actor.Actor
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.EventStreamSubscriber._

object EventStreamSubscriber {

  trait InFlightMessagesCallback {
    def onChanged(messagesInFlight: Int)
  }
}

trait EventStreamSubscriber {
  this: Actor =>

  /**
   * Subscribes this actor (the subscriber) to given event stream.
   * The subscriber will receive events as [[pl.newicom.dddd.messaging.event.EventMessageEntry]] messages.
   *
   * @param fromPositionExclusive if provided Subscriber will be receiving events
   *                              from given position (exclusively)
    * @return callback that the subscriber should invoke whenever number of messages in flight is changed.
   *         This information can be used by event publisher to control the number of emitted events.
   */
  def subscribe(observable: BusinessEntity, fromPositionExclusive: Option[Long]): InFlightMessagesCallback


}
