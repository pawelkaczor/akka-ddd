package pl.newicom.dddd.aggregate

import akka.actor.{ActorRef, Stash}

import scala.concurrent.duration._

object CollaborationSupport {
  case object ReceiveTimeout

  @SerialVersionUID(1L)
  class CollaborationFailed(msg: String) extends RuntimeException(msg)

  case class NoResponseReceived(timeout: FiniteDuration)
    extends CollaborationFailed(s"No response received within $timeout.")

  case class UnexpectedResponseReceived(response: Any)
    extends CollaborationFailed(s"Unexpected response received: $response.")

}

trait CollaborationSupport[Event <: DomainEvent] extends Stash {
  this: AggregateRoot[Event, _, _] =>
  import CollaborationSupport._

  sealed trait Eventually[E <: Event]

  case class Immediately[E <: Event](e: E) extends Eventually[E]

  implicit def toEventually(e: Event): Eventually[Event] = Immediately(e)

  implicit class CollaborationBuilder(val target: ActorRef) {
    def !<(msg: Any): Collaboration = Collaboration(target, msg, PartialFunction.empty, null)
  }

  case class Collaboration(target: ActorRef, msg: Any, receive: HandleCommand, timeout: FiniteDuration) extends Eventually[Event] {
    def apply(receive: HandleCommand)(implicit timeout: FiniteDuration): Collaboration = {
      copy(receive = receive, timeout = timeout)
    }

    def expectOnce(receive: HandleCommand)(implicit timeout: FiniteDuration): Unit = apply(receive)

    def execute(callback: Event => Unit): Unit = {
      target ! msg
      internalExpectOnce(target, receive, callback)(timeout)
    }
  }

  private def internalExpectOnce(target: ActorRef, receive: HandleCommand, callback: Event => Unit)(implicit timeout: FiniteDuration): Unit = {
    import context.dispatcher
    val scheduledTimeout = context.system.scheduler.scheduleOnce(timeout, self, ReceiveTimeout)

    context.become(
      receive.andThen { // expected response received
        case Immediately(event) => callback(event)
        case _: Collaboration => sys.error("Nested collaboration not supported")
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
