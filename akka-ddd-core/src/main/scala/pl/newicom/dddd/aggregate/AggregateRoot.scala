package pl.newicom.dddd.aggregate

import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.AggregateRootSupport.{Accept, Reaction, Reject, RejectConditionally}
import pl.newicom.dddd.aggregate.error.{AggregateRootNotInitializedException, DomainException}
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.LocalOfficeId

import scala.PartialFunction.empty
import scala.concurrent.duration._
import scala.util.Try

abstract class AggregateRootActorFactory[A <: AggregateRoot[_, _, A]: LocalOfficeId] extends BusinessEntityActorFactory[A] {
  def inactivityTimeout: Duration = 1.minute
}

trait AggregateState[S <: AggregateState[S]] {
  type StateMachine = PartialFunction[DomainEvent, S]
  def apply: StateMachine
  def eventHandlerDefined(e: DomainEvent): Boolean = apply.isDefinedAt(e)
  def initialized: Boolean = true
}

trait AggregateBehaviour[E <: DomainEvent, S <: AggregateState[S]] extends AggregateState[S] {

  type Command = Any
  type HandleCommand = PartialFunction[Command, Reaction[E]]

  def handleCommand: HandleCommand

  implicit def toReaction(e: E): Accept[E] =
    Accept(Seq(e))
  implicit def toReaction(events: Seq[E]): Accept[E] =
    Accept(events)

  def error(msg: String): Reject = reject(msg)
  def reject(msg: String): Reject = Reject(new DomainException(msg))

  def reject(reason: DomainException): Reject = Reject(reason)

  def rejectIf(condition: Boolean, reason: String): RejectConditionally = RejectConditionally(condition, reject(reason))
  def rejectIf(condition: Boolean, reject: Reject): RejectConditionally = RejectConditionally(condition, reject)
}

trait AggregateActions[E <: DomainEvent, S <: AggregateState[S]] extends AggregateBehaviour[E, S] {

  case class Actions(commandHandlers: HandleCommand, eventHandlers: StateMachine = empty) {
    def handleEvents(eh: StateMachine): Actions =
      copy(eventHandlers = eh)

    def map[SS <: S](f: SS => S): Actions =
      copy(eventHandlers = eventHandlers.asInstanceOf[PartialFunction[DomainEvent, SS]].andThen(f))

    def ++(other: Actions): Actions =
      Actions(
        commandHandlers.orElse(other.commandHandlers),
        eventHandlers.orElse(other.eventHandlers)
      )

    def orElse[SS <: S](other: AggregateActions[E, S], f: SS => S = (a: SS) => a): Actions =
      Actions(
        commandHandlers.orElse(other.handleCommand),
        eventHandlers.orElse(other.apply.asInstanceOf[PartialFunction[DomainEvent, SS]].andThen(f))
      )

    def orElse(other: Actions): Actions =
      Actions(commandHandlers.orElse(other.commandHandlers), eventHandlers.orElse(other.eventHandlers))
  }

  def handleCommand: HandleCommand =
    actions.commandHandlers

  def apply: StateMachine =
    actions.eventHandlers

  protected def actions: Actions

  protected def handleCommands(hc: HandleCommand): Actions =
    Actions(hc)

  protected def noActions: Actions = Actions(empty)
}

trait Uninitialized[S <: AggregateState[S]] {
  this: AggregateState[S] =>
  override def initialized = false
}

abstract class AggregateRoot[Event <: DomainEvent, S <: AggregateState[S] : Uninitialized, A <: AggregateRoot[Event, S, A] : LocalOfficeId] extends AggregateRootBase with CollaborationSupport[Event] {

  type HandleCommand = PartialFunction[Any, Reaction[Event]]

  override def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
  override def department: String = officeId.department

  private lazy val sm = new StateManager(onStateChanged = messageProcessed)

  def initialized: Boolean = state.initialized

  def state: S = sm.state

  override def receiveCommand: Receive = {
    case cm: CommandMessage =>
      Try {
        handleCommand.orElse(handleUnknown).andThen(execute)(cm.command)
      }.recover {
        case ex: DomainException => execute(Reject(ex))
      }
  }

  private def execute(r: Reaction[Event]): Unit = r match {
    case c: Collaboration => c.execute(raise)
    case Accept(events) => raise(events)
    case Reject(ex) => acknowledgeCommandRejected(ex)
  }

  def handleUnknown: HandleCommand = {
    case cmd =>
      val commandName = cmd.getClass.getSimpleName
      if (initialized) {
        Reject(new DomainException(s"$commandName can not be processed: missing command handler!"))
      } else {
        val caseName = officeId.caseName
        Reject(new AggregateRootNotInitializedException(s"$caseName with ID $id does not exist. $commandName can not be processed: missing command handler!"))
      }
  }

  override def receiveRecover: Receive = {
    case em: EventMessage => sm.apply(em)
  }

  def handleCommand: HandleCommand =
    state.asInstanceOf[AggregateBehaviour[Event, S]].handleCommand

  private def raise(events: Seq[Event]): Unit = {
    var eventsCount = 0
    val eventMessages = events.map(toEventMessage).map(_.causedBy(currentCommandMessage))

    val handler =
      sm.eventMessageHandler.andThen { _ =>
        eventsCount += 1
        if (eventsCount == events.size) {
           handle(currentCommandSender, eventMessages.map(toOfficeEventMessage))
        }
      }

    persistAll(eventMessages.toList)(e => safely(handler(e)))
  }

  // do not escalate DomainException
  private def safely(f: => Any): Unit = try f catch {
    case ex: DomainException =>
      acknowledgeCommandRejected(ex)
  }


  private class StateManager(onStateChanged: (EventMessage) => Unit) {
    private var s: S = implicitly[Uninitialized[S]].asInstanceOf[S]

    def state: S = s

    def apply(em: EventMessage): Unit = {
      apply(eventHandler, em)
    }

    def eventMessageHandler: (EventMessage) => EventMessage = em => {
      apply(eventHandler, em)
      em
    }

    private def apply(eventHandler: Function[DomainEvent, S], em: EventMessage): Unit = {
      s = eventHandler(em.event)
      onStateChanged(em)
    }

    private def eventHandler: Function[DomainEvent, S] = event => {
      def eventName = event.getClass.getSimpleName
      def commandName = currentCommandMessage.command.getClass.getSimpleName
      def caseName = officeId.caseName
      s match {
        case state if state.eventHandlerDefined(event) =>
          state.apply(event)
        case state if state.initialized =>
          throw new DomainException(s"$commandName can not be processed. State transition not defined for event: $eventName!")
        case _ =>
          throw new AggregateRootNotInitializedException(s"$caseName with ID $id does not exist. $commandName can not be processed: missing state initialization for event: $eventName!")
      }
    }

  }

}
