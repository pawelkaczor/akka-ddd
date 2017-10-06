package pl.newicom.dddd.aggregate

import pl.newicom.dddd.aggregate.AggregateRootSupport.{AcceptConditionally, Reaction, Reject, RejectConditionally}
import pl.newicom.dddd.aggregate.error.DomainException

trait BehaviorSupport[E <: DomainEvent] {
  def error(msg: String): Reject  =
    reject(msg)

  def reject(msg: String): Reject =
    Reject(new DomainException(msg))

  def reject(reason: DomainException): Reject =
    Reject(reason)

  def rejectIf(condition: Boolean, reason: String): RejectConditionally =
    new RejectConditionally(condition, reject(reason))

  def rejectIf(condition: Boolean, reject: => Reject): RejectConditionally =
    new RejectConditionally(condition, reject)

  def acceptIf(condition: Boolean)(reaction: => Reaction[E]): AcceptConditionally[E] =
    new AcceptConditionally(condition, reaction)
}
