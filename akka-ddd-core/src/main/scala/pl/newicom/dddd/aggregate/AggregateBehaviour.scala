package pl.newicom.dddd.aggregate

import pl.newicom.dddd.aggregate.AggregateRootSupport.{AcceptC, AcceptQ, Reaction}

trait AggregateBehaviour[E <: DomainEvent, S <: AggregateState[S], C <: Config] extends AggregateState[S] with AggregateBehaviourSupport {

  type HandleQuery              = PartialFunction[Query, Reaction[_]]
  type HandleCommand            = PartialFunction[Command, Reaction[E]]
  type HandleCommandWithContext = CommandHandlerContext[C] => HandleCommand

  def commandHandler: HandleCommandWithContext
  def commandHandlerNoCtx: HandleCommand = commandHandler(null)

  def qHandler: HandleQuery

  implicit def toReaction(e: E): AcceptC[E] =
    AcceptC(Seq(e))
  implicit def toReaction(events: Seq[E]): AcceptC[E] =
    AcceptC(events)

  def reply[R](r: R): AcceptQ[R] = AcceptQ[R](r)
}
