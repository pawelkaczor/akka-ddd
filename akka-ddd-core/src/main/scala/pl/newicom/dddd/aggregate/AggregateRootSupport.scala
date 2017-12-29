package pl.newicom.dddd.aggregate

import pl.newicom.dddd.aggregate.error.DomainException
import pl.newicom.dddd.office.{LocalOfficeId, OfficeListener}

object AggregateRootSupport {

  trait Reaction[+E]

  case class AcceptC[E <: DomainEvent](events: Seq[E]) extends Reaction[E] {
    def &(next: E): AcceptC[E] = AcceptC(events :+ next)
    def &(next: Seq[E]): AcceptC[E] = AcceptC(events ++ next)
  }

  case class AcceptQ[R](response: R) extends Reaction[R]

  object Reject {
    private[aggregate] def apply(reason: Throwable): Reject = new Reject(reason)
    def unapply(arg: Reject): Option[Throwable] = Some(arg.reason)
  }

  class Reject private[aggregate] (val reason: Throwable) extends Reaction[Nothing]

  class RejectConditionally(condition: Boolean, reject: => Reject) {
    def orElse[E <: DomainEvent](reaction: => Reaction[E]): Reaction[E] =
      if (condition) reject else reaction

    def orElse(alternative: => RejectConditionally): RejectConditionally =
      if (condition) this else alternative

    def isRejected: Boolean = !condition
  }

  class AcceptConditionally[E <: DomainEvent](condition: Boolean, reaction: => Reaction[E]) {
    def orElse(rejectionReason: String): Reaction[E] =
      orElse(new DomainException(rejectionReason))

    def orElse(rejectionReason: => DomainException): Reaction[E] =
      if (condition) reaction else Reject(rejectionReason)

    def isAccepted: Boolean = condition
  }


}

trait AggregateRootSupport extends BehaviorSupport[DomainEvent] {

  implicit def officeListener[A <: AggregateRoot[_, _, _] : LocalOfficeId]: OfficeListener[A] = new OfficeListener[A]

}
