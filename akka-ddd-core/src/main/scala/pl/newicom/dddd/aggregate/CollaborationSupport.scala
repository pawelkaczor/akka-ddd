package pl.newicom.dddd.aggregate

import akka.actor.{ActorRef, Stash}
import pl.newicom.dddd.aggregate.AggregateRootSupport.{AcceptC, Reaction}
import pl.newicom.dddd.aggregate.error.{NoResponseReceived, UnexpectedResponseReceived}

import scala.concurrent.duration._

object CollaborationSupport {
  case object ReceiveTimeout
}

trait CollaborationSupport[Event <: DomainEvent] extends Stash {
  this: AggregateRoot[Event, _, _] =>
  import CollaborationSupport._
  type HandleResponse = PartialFunction[Any, Reaction[Event]]

  implicit def toReaction(e: Event): AcceptC[Event] = AcceptC(Seq(e))

  implicit class CollaborationBuilder(val target: ActorRef) {
    def !<(msg: Any): Collaboration = Collaboration(target, msg, PartialFunction.empty, null)
  }

  case class Collaboration(target: ActorRef, msg: Any, receive: HandleResponse, timeout: FiniteDuration) extends Reaction[Event] {
    def apply(receive: HandleResponse)(implicit timeout: FiniteDuration): Collaboration = {
      copy(receive = receive, timeout = timeout)
    }

    def expectOnce(receive: HandleResponse)(implicit timeout: FiniteDuration): Unit = apply(receive)

    def execute(callback: Reaction[Event] => Unit): Unit = {
      target ! msg
      internalExpectOnce(target, receive, callback)(timeout)
    }
  }

  private def internalExpectOnce(target: ActorRef, receive: HandleResponse, callback: Reaction[Event] => Unit)(implicit timeout: FiniteDuration): Unit = {
    import context.dispatcher
    val scheduledTimeout = context.system.scheduler.scheduleOnce(timeout, self, ReceiveTimeout)

    context.become(
      receive.andThen(callback).andThen { _ =>
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
