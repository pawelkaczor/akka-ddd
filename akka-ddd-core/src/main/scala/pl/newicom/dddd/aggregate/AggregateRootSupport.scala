package pl.newicom.dddd.aggregate

import pl.newicom.dddd.aggregate.error.DomainException
import pl.newicom.dddd.office.{LocalOfficeId, OfficeListener}
import pl.newicom.dddd.utils.ImplicitUtils._

import scala.concurrent.duration.FiniteDuration

object AggregateRootSupport {

  sealed trait AbstractReaction[+R]

  sealed trait Reaction[+E] extends AbstractReaction[Seq[E]] {
    def flatMap[B](f: Seq[E] => Reaction[B]): Reaction[B]

    def flatMapMatching[B](f: PartialFunction[E, Reaction[B]]): Reaction[B] =
      flatMap {
        case Seq(e) if f.isDefinedAt(e) =>
          f(e)
        case Seq(e, _*) if f.isDefinedAt(e) =>
          f(e)
        case _ =>
          this.asParameterizedBy[B]
      }

    def recoverWith[B](f: => Reaction[B]): Reaction[B]

    def reversed: Reaction[E] = this
  }

  case object NoReaction extends Reaction[Nothing] {
    def flatMap[B](f: Seq[Nothing] => Reaction[B]): Reaction[B] =
      f(Seq())

    def recoverWith[B](f: => Reaction[B]): Reaction[B] =
      f
  }

  trait Collaborate[E] extends Reaction[E] {
    type HandleResponse = PartialFunction[Any, Reaction[E]]
    def apply(receive: HandleResponse)(implicit timeout: FiniteDuration): Collaborate[E]
  }

  case class AcceptC[E](events: Seq[E]) extends Reaction[E] {

    def &(next: E): AcceptC[E] = AcceptC(events :+ next)
    def &(next: Seq[E]): AcceptC[E] = AcceptC(events ++ next)

    override def reversed: Reaction[E] =
      copy(events.reverse)

    def flatMap[B](f: Seq[E] => Reaction[B]): Reaction[B] = {
      (f(events).asParameterizedBy[E] match {
        case AcceptC(es) =>
          AcceptC(events ++ es)
        case c: Collaborate[E] =>
          c.flatMap(_ => this).reversed
        case r =>
          r.flatMap(_ => this)

      }).asParameterizedBy[B]
    }

    def recoverWith[B](f: => Reaction[B]): Reaction[B] =
      this.asParameterizedBy[B]

  }

  case class AcceptQ[R](response: R) extends AbstractReaction[R]

  object Reject {
    private[aggregate] def apply(reason: Throwable): Reject = new Reject(reason)
    def unapply(arg: Reject): Option[Throwable] = Some(arg.reason)
  }

  class Reject private[aggregate] (val reason: Throwable) extends Reaction[Nothing] {
    def flatMap[B](f: Seq[Nothing] => Reaction[B]): Reaction[B] = this
    def recoverWith[B](f: => Reaction[B]): Reaction[B] = f
  }

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
