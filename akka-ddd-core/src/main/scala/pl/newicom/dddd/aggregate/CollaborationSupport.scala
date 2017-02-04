package pl.newicom.dddd.aggregate

import akka.actor.{ActorRef, Stash}
import pl.newicom.dddd.aggregate.AggregateRootSupport.{Eventually, Immediately}
import pl.newicom.dddd.aggregate.error.DomainException

import scala.concurrent.duration._

object CollaborationSupport {
  case object ReceiveTimeout

  @SerialVersionUID(1L)
  class CollaborationFailed(msg: String) extends DomainException(msg)

  case class NoResponseReceived(timeout: FiniteDuration)
    extends CollaborationFailed(s"No response received within $timeout.")

  case class UnexpectedResponseReceived(response: Any)
    extends CollaborationFailed(s"Unexpected response received: $response.")

}


trait CollaborationSupport[Event <: DomainEvent] extends Stash {
  this: AggregateRoot[Event, _, _] =>
  import CollaborationSupport._

  implicit def toEventually(e: Event): Immediately[Event] = Immediately(Seq(e))

  implicit class CollaborationBuilder(val target: ActorRef) {
    def !<(msg: Any): Collaboration = Collaboration(target, msg, PartialFunction.empty, null)
  }

  case class Collaboration(target: ActorRef, msg: Any, receive: HandleCommand, timeout: FiniteDuration) extends Eventually[Event] {
    def apply(receive: HandleCommand)(implicit timeout: FiniteDuration): Collaboration = {
      copy(receive = receive, timeout = timeout)
    }

    def expectOnce(receive: HandleCommand)(implicit timeout: FiniteDuration): Unit = apply(receive)

    def execute(callback: Seq[Event] => Unit): Unit = {
      target ! msg
      internalExpectOnce(target, receive, callback)(timeout)
    }
  }

  private def internalExpectOnce(target: ActorRef, receive: HandleCommand, callback: Seq[Event] => Unit)(implicit timeout: FiniteDuration): Unit = {
    import context.dispatcher
    val scheduledTimeout = context.system.scheduler.scheduleOnce(timeout, self, ReceiveTimeout)

    context.become(
      receive.andThen { // expected response received
        case Immediately(events) => callback(events)
        case c: Collaboration => c.execute(callback)
      }.andThen { _ =>
        scheduledTimeout.cancel()
        unstashAll()
        context.unbecome()
      } orElse {
        case ReceiveTimeout =>
          throw NoResponseReceived(timeout)

        case msg if sender() eq target =>
          throw UnexpectedResponseReceived(msg)

        case _  =>
          stash()
      }
      , discardOld = false)
  }

}
