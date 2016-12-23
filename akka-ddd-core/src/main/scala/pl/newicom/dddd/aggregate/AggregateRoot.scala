package pl.newicom.dddd.aggregate

import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.error.AggregateRootNotInitializedException
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.LocalOfficeId
import scala.concurrent.duration.{Duration, _}

trait AggregateState[T <: AggregateState[T]] {
  type StateMachine = PartialFunction[DomainEvent, T]
  def apply: StateMachine
  def eventHandlerDefined(e: DomainEvent): Boolean = apply.isDefinedAt(e)

}

abstract class AggregateRootActorFactory[A <: AggregateRoot[_, A]: LocalOfficeId] extends BusinessEntityActorFactory[A] {
  def inactivityTimeout: Duration = 1.minute
}


abstract class AggregateRoot[S <: AggregateState[S], A <: AggregateRoot[S, A] : LocalOfficeId] extends AggregateRootBase {

  override def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
  override def department: String = officeId.department

  type AggregateRootFactory = PartialFunction[DomainEvent, S]

  val factory: AggregateRootFactory

  private lazy val sm = StateManager(factory, onStateChanged = messageProcessed)

  def initialized: Boolean = sm.initialized

  def state: S = sm.state

  override def receiveCommand: Receive = {
    case cm: CommandMessage => handleCommand.applyOrElse(cm.command, unhandled)
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


  case class StateManager(factory: AggregateRootFactory, onStateChanged: (EventMessage) => Unit) {
    private var s: Option[S] = None

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

    def initialized: Boolean = s.isDefined

    def state: S = if (initialized) s.get else throw new AggregateRootNotInitializedException

    private def apply(eventHandler: Function[DomainEvent, S], em: EventMessage): Unit = {
      s = Some(eventHandler(em.event))
      onStateChanged(em)
    }

    private def eventHandler(event: DomainEvent): Function[DomainEvent, S] = {
      def eventName = event.getClass.getSimpleName
      def commandName = currentCommandMessage.command.getClass.getSimpleName
      def arName = officeId.clerkClass.getSimpleName
      s match {
        case Some(state) if state.eventHandlerDefined(event) =>
          state.apply
        case Some(_) =>
          sys.error(s"$commandName can not be processed. State transition not defined for event: $eventName!")
        case None if factory.isDefinedAt(event) =>
          factory
        case _ =>
          throw new AggregateRootNotInitializedException(s"$arName with ID $id does not exist. $commandName can not be processed: missing state initialization for event: $eventName!")
      }
    }

  }

}
