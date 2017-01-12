package pl.newicom.dddd.aggregate

import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.error.AggregateRootNotInitializedException
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.LocalOfficeId

import scala.concurrent.duration.{Duration, _}

abstract class AggregateRootActorFactory[A <: AggregateRoot[_, A]: LocalOfficeId] extends BusinessEntityActorFactory[A] {
  def inactivityTimeout: Duration = 1.minute
}


trait AggregateState[S <: AggregateState[S]] {
  type StateMachine = PartialFunction[DomainEvent, S]
  def apply: StateMachine
  def eventHandlerDefined(e: DomainEvent): Boolean = apply.isDefinedAt(e)
  def initialized: Boolean
}

trait Initialized[S <: AggregateState[S]] {
  this: AggregateState[S] =>
  override def initialized = true
}

trait Uninitialized[S <: AggregateState[S]] {
  this: AggregateState[S] =>
  override def initialized = false
}

abstract class AggregateRoot[S <: AggregateState[S] : Uninitialized, A <: AggregateRoot[S, A] : LocalOfficeId] extends AggregateRootBase {

  override def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
  override def department: String = officeId.department

  private lazy val sm = new StateManager(onStateChanged = messageProcessed)

  def initialized: Boolean = state.initialized

  def state: S = sm.state

  override def receiveCommand: Receive = {
    case cm: CommandMessage => handleCommand.applyOrElse(cm.command, unhandledCommand)
  }

  def unhandledCommand(command: Any): Unit = {
    val commandName = command.getClass.getSimpleName
    if (initialized) {
      sys.error(s"$commandName can not be processed: missing command handler!")
    } else {
      val arName = officeId.clerkClass.getSimpleName
      throw new AggregateRootNotInitializedException(s"$arName with ID $id does not exist. $commandName can not be processed: missing command handler!")
    }
  }

  override def receiveRecover: Receive = {
    case em: EventMessage => sm.apply(em)
  }

  def handleCommand: Receive

  def raise(event: DomainEvent) {
    val handler = sm.eventMessageHandler(event).andThen { em =>
      handle(currentCommandSender, toOfficeEventMessage(em))
    }

    val em = toEventMessage(event).causedBy(currentCommandMessage)

    persist(em)(handler)
  }


  class StateManager(onStateChanged: (EventMessage) => Unit) {
    private var s: S = implicitly[Uninitialized[S]].asInstanceOf[S]

    def state: S = s

    def apply(em: EventMessage): Unit = {
      apply(eventHandler(em.event), em)
    }

    def eventMessageHandler(event: DomainEvent): (EventMessage) => EventMessage = {
      val eh = eventHandler(event)
      (em: EventMessage) => {
        apply(eh, em)
        em
      }
    }

    private def apply(eventHandler: Function[DomainEvent, S], em: EventMessage): Unit = {
      s = eventHandler(em.event)
      onStateChanged(em)
    }

    private def eventHandler(event: DomainEvent): Function[DomainEvent, S] = {
      def eventName = event.getClass.getSimpleName
      def commandName = currentCommandMessage.command.getClass.getSimpleName
      def arName = officeId.clerkClass.getSimpleName
      s match {
        case state if state.eventHandlerDefined(event) =>
          state.apply
        case state if state.initialized =>
          sys.error(s"$commandName can not be processed. State transition not defined for event: $eventName!")
        case _ =>
          throw new AggregateRootNotInitializedException(s"$arName with ID $id does not exist. $commandName can not be processed: missing state initialization for event: $eventName!")
      }
    }

  }

}
