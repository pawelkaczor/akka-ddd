package pl.newicom.dddd.aggregate

import pl.newicom.dddd.aggregate.error.CommandRejected
import pl.newicom.dddd.office.{LocalOfficeId, OfficeListener}

object AggregateRootSupport {

  trait Reaction[+E <: DomainEvent]

  case class Accept[E <: DomainEvent](events: Seq[E]) extends Reaction[E] {
    def &(next: E): Accept[E] = Accept(events :+ next)
  }

  object Reject {
    def apply(reason: CommandRejected): Reject = new Reject(reason)
    def unapply(arg: Reject): Option[Throwable] = Some(arg.reason)
  }

  class Reject private[aggregate] (val reason: Throwable) extends Reaction[Nothing]

  class RejectConditionally(condition: Boolean, reject: => Reject) {
    def orElse[E <: DomainEvent](accept: => Accept[E]): Reaction[E] =
      if (condition) reject else accept
  }

}

trait AggregateRootSupport {

  implicit def officeListener[A <: AggregateRoot[_, _, _] : LocalOfficeId]: OfficeListener[A] = new OfficeListener[A]

}
