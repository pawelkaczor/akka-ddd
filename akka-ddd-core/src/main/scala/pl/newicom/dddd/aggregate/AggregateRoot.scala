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
}

abstract class AggregateRootActorFactory[A <: AggregateRoot[_, A]: LocalOfficeId] extends BusinessEntityActorFactory[A] {
  def inactivityTimeout: Duration = 1.minute
}


abstract class AggregateRoot[S <: AggregateState[S], A <: AggregateRoot[S, A] : LocalOfficeId] extends AggregateRootBase {

  override def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]

  type AggregateRootFactory = PartialFunction[DomainEvent, S]

  val factory: AggregateRootFactory

  private lazy val sm = StateManager(factory, onStateChanged = messageProcessed)

  def initialized = sm.initialized

  def state = sm.state

  override def receiveCommand: Receive = {
    case cm: CommandMessage => handleCommand.applyOrElse(cm.command, unhandled)
  }

  override def receiveRecover: Receive = {
    case em: EventMessage => sm.apply(em)
  }

  def handleCommand: Receive

  def raise(event: DomainEvent) {
    persist(EventMessage(event).causedBy(currentCommandMessage)) {
      persisted =>
        {
          sm.apply(persisted)
          handle(currentCommandSender, toOfficeEventMessage(persisted))
        }
    }
  }

  private case class StateManager(factory: AggregateRootFactory, onStateChanged: (EventMessage) => Unit) {
    private var s: Option[S] = None

    def apply(em: EventMessage): Unit = {
      s = s match {
        case Some(as) => Some(as.apply(em.event))
        case None => Some(factory.apply(em.event))
      }
      onStateChanged(em)
    }

    def initialized = s.isDefined

    def state = if (initialized) s.get else throw new AggregateRootNotInitializedException
  }

}
