package pl.newicom.dddd.aggregate

import pl.newicom.dddd.actor.Config
import pl.newicom.dddd.aggregate.AggregateRootSupport._
import pl.newicom.dddd.aggregate.error.CommandHandlerNotDefined

import scala.PartialFunction.empty
import scala.reflect.ClassTag

trait Behavior[E <: DomainEvent, S <: AggregateState[S], C <: Config] extends AggregateState[S] with BehaviorSupport[E]  {

  type HandleQuery              = PartialFunction[Query, AbstractReaction[_]]
  type HandleCommand            = PartialFunction[Command, Reaction[E]]
  type HandleCommandWithContext = CommandHandlerContext[C] => HandleCommand

  implicit def toReaction(e: E): AcceptC[E] =
    AcceptC(Seq(e))
  implicit def toReaction(events: Seq[E]): AcceptC[E] =
    AcceptC(events)

  case class Actions(cHandler: HandleCommandWithContext, qHandler: HandleQuery = empty, eventHandler: StateMachine = empty) {
    def handleEvent(eh: StateMachine): Actions =
      copy(eventHandler = eh)

    def handleQuery[Q <: Query: ClassTag](hq: Function[Q, AbstractReaction[Q#R]]): Actions = {
      val pf: HandleQuery = { case x: Q => hq(x) }
      copy(qHandler = qHandler.orElse(pf))
    }

    def map[SS <: S](f: SS => S): Actions =
      copy(eventHandler = eventHandler.asInstanceOf[PartialFunction[DomainEvent, SS]].andThen(f))

    def ++(other: Actions): Actions =
      Actions(ctx => cHandler(ctx).orElse(other.cHandler(ctx)), qHandler.orElse(other.qHandler), eventHandler.orElse(other.eventHandler))

    def orElse[SS <: S](other: Behavior[E, S, C], f: SS => S = (a: SS) => a): Actions =
      Actions(
        ctx => cHandler(ctx).orElse(other.commandHandlerNoCtx),
        qHandler.orElse(other.qHandler),
        eventHandler.orElse(other.eventHandler.asInstanceOf[PartialFunction[DomainEvent, SS]].andThen(f))
      )

    def orElse(other: Actions): Actions =
      Actions(ctx => cHandler(ctx).orElse(other.cHandler(ctx)), qHandler.orElse(other.qHandler), eventHandler.orElse(other.eventHandler))
  }

  def orElse[SS <: S, B <: Behavior[E, S, C]](other: Behavior[E, S, C]): B#Actions =
    actions.orElse(other).asInstanceOf[B#Actions]

  def commandHandlerNoCtx: HandleCommand =
    commandHandler(null)

  def commandHandler: HandleCommandWithContext =
    actions.cHandler

  def apply(command: Command): Reaction[E] =
    if (commandHandlerNoCtx.isDefinedAt(command)) {
      commandHandlerNoCtx(command)
    } else {
      Reject(new CommandHandlerNotDefined(command.getClass.getSimpleName))
    }

  def apply(event: E): S =
    eventHandler(event)

  override def eventHandler: StateMachine =
    actions.eventHandler

  def qHandler: HandleQuery =
    actions.qHandler

  def reply[R](r: R): AcceptQ[R] = AcceptQ[R](r)

  protected def actions: Actions

  def withContext(ctxConsumer: (CommandHandlerContext[C]) => Actions): Actions = {
    Actions(ctx => ctxConsumer(ctx).cHandler(null))
  }

  def handleCommand(hc: HandleCommand): Actions =
    Actions(_ => hc)

  protected def noActions: Actions = Actions(empty)
}
