package pl.newicom.dddd.aggregate

import akka.actor.{ActorRef, Stash}
import pl.newicom.dddd.aggregate.AggregateRootSupport._
import pl.newicom.dddd.aggregate.error.{NoResponseReceived, UnexpectedResponseReceived}
import akka.actor.Timers

import scala.concurrent.duration._

object CollaborationSupport {
  private case object TimeoutKey
  case object ReceiveTimeout
}

trait CollaborationSupport[Event <: DomainEvent] extends Stash with Timers {
  this: AggregateRoot[Event, _, _] =>
  import CollaborationSupport._
  type HandleResponse = PartialFunction[Any, Reaction[Event]]

  implicit def toReaction(e: Event): AcceptC[Event] = AcceptC(Seq(e))

  implicit class CollaborationBuilder(val target: ActorRef) {
    def !<(msg: Any): Collaborate[Event] = Collaboration(target, msg)
  }

  case class Collaboration(
                            target: ActorRef,
                            msg: Any,
                            receive: HandleResponse = PartialFunction.empty,
                            timeout: FiniteDuration = 3.seconds,
                            mapper: Option[Seq[Event] => Reaction[Event]] = None,
                            recovery: () => Reaction[Event] = () => NoReaction,
                            reverse: Boolean = false)
    extends Collaborate[Event] {

    def apply(receive: HandleResponse)(implicit timeout: FiniteDuration): Collaborate[Event] = {
      copy(receive = receive, timeout = timeout)
    }

    def expectOnce(receive: HandleResponse)(implicit timeout: FiniteDuration): Unit = apply(receive)

    def execute(callback: Reaction[Event] => Unit): Unit = {
      target ! msg
      internalExpectOnce(target, receive, r => callback {
        Some {
          mapper.map(r.flatMap).getOrElse(r)
            .recoverWith(recovery())
        }.map {
          case rr if reverse => rr.reversed
          case rr => rr
        }.get
      })(timeout)
    }

    def flatMap[B](f: Seq[Event] => Reaction[B]): Reaction[B] = {
      def ff = f.andThen(_.asInstanceOf[Reaction[Event]])
      copy(mapper = Some(mapper.map(m => (es: Seq[Event]) => m(es).flatMap(ff)).getOrElse(ff))).asInstanceOf[Reaction[B]]
    }

    def recoverWith[B](f: => Reaction[B]): Reaction[B] =
      copy(recovery = () => f.asInstanceOf[Reaction[Event]]).asInstanceOf[Reaction[B]]

    override def reversed: Reaction[Event] =
      copy(reverse = true)
  }

  private def internalExpectOnce(target: ActorRef, receive: HandleResponse, callback: Reaction[Event] => Unit)(implicit timeout: FiniteDuration): Unit = {
    timers.startSingleTimer(TimeoutKey, ReceiveTimeout, timeout)

    context.become(
      receive.andThen { reaction =>
        timers.cancel(TimeoutKey)
        unstashAll()
        context.unbecome()
        callback(reaction)
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
