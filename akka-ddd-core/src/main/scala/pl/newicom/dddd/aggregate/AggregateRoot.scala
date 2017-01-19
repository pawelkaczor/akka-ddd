package pl.newicom.dddd.aggregate

import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.error.AggregateRootNotInitializedException
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.LocalOfficeId

import scala.concurrent.duration._

abstract class AggregateRootActorFactory[A <: AggregateRoot[_, _, A]: LocalOfficeId] extends BusinessEntityActorFactory[A] {
  def inactivityTimeout: Duration = 1.minute
}

trait AggregateState[S <: AggregateState[S]] {
  type StateMachine = PartialFunction[DomainEvent, S]
  def apply: StateMachine
  def eventHandlerDefined(e: DomainEvent): Boolean = apply.isDefinedAt(e)
  def initialized: Boolean = true
}

trait Uninitialized[S <: AggregateState[S]] {
  this: AggregateState[S] =>
  override def initialized = false
}

abstract class AggregateRoot[Event <: DomainEvent, S <: AggregateState[S] : Uninitialized, A <: AggregateRoot[Event, S, A] : LocalOfficeId] extends AggregateRootBase with CollaborationSupport[Event] {

  type HandleCommand = PartialFunction[Any, Eventually[Event]]

  override def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
  override def department: String = officeId.department

  private lazy val sm = new StateManager(onStateChanged = messageProcessed)

  def initialized: Boolean = state.initialized

  def state: S = sm.state

  override def receiveCommand: Receive = {
    case cm: CommandMessage =>
      handleCommand.andThen {
        case c: Collaboration => c.execute(raise)
        case Immediately(events) => raise(events)
      }.applyOrElse(cm.command, unhandledCommand)
  }

  def unhandledCommand(command: Any): Unit = {
    val commandName = command.getClass.getSimpleName
    if (initialized) {
      sys.error(s"$commandName can not be processed: missing command handler!")
    } else {
      val caseName = officeId.caseName
      throw new AggregateRootNotInitializedException(s"$caseName with ID $id does not exist. $commandName can not be processed: missing command handler!")
    }
  }

  override def receiveRecover: Receive = {
    case em: EventMessage => sm.apply(em)
  }

  def handleCommand: HandleCommand

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

    persistAll(eventMessages.toList)(handler)
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
          sys.error(s"$commandName can not be processed. State transition not defined for event: $eventName!")
        case _ =>
          throw new AggregateRootNotInitializedException(s"$caseName with ID $id does not exist. $commandName can not be processed: missing state initialization for event: $eventName!")
      }
    }

  }

}
