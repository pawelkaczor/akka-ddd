package pl.newicom.dddd.aggregate

import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.AggregateRootSupport.{Accept, Reaction, Reject, RejectConditionally}
import pl.newicom.dddd.aggregate.error._
import pl.newicom.dddd.messaging.Message
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.{EventMessage, OfficeEventMessage}
import pl.newicom.dddd.office.LocalOfficeId

import scala.PartialFunction.empty
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

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

  type HandleCommand = PartialFunction[Command, Reaction[E]]
  type HandleCommandMessage = PartialFunction[CommandMessage, Reaction[E]]

  def cmHandler: HandleCommandMessage
  def commandHandler: HandleCommand = {
    case c: Command if cmHandler.isDefinedAt(CommandMessage(c, "")) => cmHandler(CommandMessage(c, ""))
  }

  implicit def toReaction(e: E): Accept[E] =
    Accept(Seq(e))
  implicit def toReaction(events: Seq[E]): Accept[E] =
    Accept(events)

  def error(msg: String): Reject = reject(msg)
  def reject(msg: String): Reject = Reject(new DomainException(msg))

  def reject(reason: DomainException): Reject = Reject(reason)

  def rejectIf(condition: Boolean, reason: String): RejectConditionally = new RejectConditionally(condition, reject(reason))
  def rejectIf(condition: Boolean, reject: => Reject): RejectConditionally = new RejectConditionally(condition, reject)
}

trait AggregateActions[E <: DomainEvent, S <: AggregateState[S]] extends AggregateBehaviour[E, S] {

  case class Actions(cmHandler: HandleCommandMessage, eventHandler: StateMachine = empty) {
    def handleEvent(eh: StateMachine): Actions =
      copy(eventHandler = eh)

    def map[SS <: S](f: SS => S): Actions =
      copy(eventHandler = eventHandler.asInstanceOf[PartialFunction[DomainEvent, SS]].andThen(f))

    def ++(other: Actions): Actions =
      Actions(
        cmHandler.orElse(other.cmHandler),
        eventHandler.orElse(other.eventHandler)
      )

    def orElse[SS <: S](other: AggregateActions[E, S], f: SS => S = (a: SS) => a): Actions =
      Actions(
        cmHandler.orElse(other.cmHandler),
        eventHandler.orElse(other.apply.asInstanceOf[PartialFunction[DomainEvent, SS]].andThen(f))
      )

    def orElse(other: Actions): Actions =
      Actions(cmHandler.orElse(other.cmHandler), eventHandler.orElse(other.eventHandler))
  }

  def cmHandler: HandleCommandMessage =
    actions.cmHandler

  def apply: StateMachine =
    actions.eventHandler

  protected def actions: Actions

  def handleCommand(hc: HandleCommand): Actions =
    Actions { case cm: CommandMessage if hc.isDefinedAt(cm.command) => hc(cm.command)}

  protected def handleCommandMessage(hcm: HandleCommandMessage): Actions =
    Actions(hcm)

  protected def noActions: Actions = Actions(empty)
}

trait Uninitialized[S <: AggregateState[S]] {
  this: AggregateState[S] =>
  override def initialized = false
}

abstract class AggregateRoot[Event <: DomainEvent, S <: AggregateState[S] : Uninitialized, A <: AggregateRoot[Event, S, A] : LocalOfficeId] extends AggregateRootBase with CollaborationSupport[Event] {

  type HandleCommand = PartialFunction[Command, Reaction[Event]]
  type HandleCommandMessage = PartialFunction[CommandMessage, Reaction[Event]]

  override def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
  override def department: String = officeId.department

  private lazy val sm = new StateManager(onStateChanged = messageProcessed)

  def initialized: Boolean = state.initialized

  def state: S = sm.state

  override def preRestart(reason: Throwable, msgOpt: Option[Any]) {
    reply(Failure(reason))
    super.preRestart(reason, msgOpt)
  }

  override def receiveCommand: Receive = {
    case cm: CommandMessage =>
      safely {
        handleCommandMessage.orElse(handleUnknown).andThen(execute)(cm)
      }
  }

  private def execute(r: Reaction[Event]): Unit = r match {
    case c: Collaboration => c.execute(raise)
    case Accept(events) => raise(events)
    case Reject(ex) => reply(Failure(ex))
  }

  def handleUnknown: HandleCommandMessage = {
    case cm: CommandMessage =>
      val commandName = cm.command.getClass.getSimpleName
      Reject(
        if (initialized)
          new CommandHandlerNotDefined(commandName)
        else
          new AggregateRootNotInitialized(officeId.caseName, id, commandName)
      )
  }

  override def receiveRecover: Receive = {
    case em: EventMessage => sm.apply(em)
  }

  def handleCommandMessage: HandleCommandMessage =
    state.asInstanceOf[AggregateBehaviour[Event, S]].cmHandler

  private def raise(events: Seq[Event]): Unit = {
    var eventsCount = 0
    val eventMessages = events.map(toEventMessage).map(_.causedBy(currentCommandMessage))

    val handler =
      sm.eventMessageHandler.andThen { _ =>
        eventsCount += 1
        if (eventsCount == events.size) {
          val oems = eventMessages.map(toOfficeEventMessage)
          reply(Success(oems))
        }
      }

    persistAll(eventMessages.toList)(e => safely(handler(e)))
  }

  private def reply(result: Try[Seq[OfficeEventMessage]], cm: CommandMessage = currentCommandMessage) {
    currentCommandSender ! cm.deliveryReceipt(result.map(successMapper))
  }

  def handleDuplicated(msg: Message): Unit =
    reply(Success(Seq.empty), msg.asInstanceOf[CommandMessage])

  private def safely(f: => Unit): Unit = try f catch {
      case ex: Throwable => execute(new Reject(ex))
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
          throw new StateTransitionNotDefined(commandName, eventName)
        case _ =>
          throw new AggregateRootNotInitialized(caseName, id, commandName, Some(eventName))
      }
    }

  }

}
