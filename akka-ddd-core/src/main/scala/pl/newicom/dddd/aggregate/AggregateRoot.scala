package pl.newicom.dddd.aggregate

import akka.persistence.{RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import akka.persistence.journal.Tagged
import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.aggregate.AggregateRootSupport._
import pl.newicom.dddd.aggregate.error._
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.{EventMessage, OfficeEventMessage}
import pl.newicom.dddd.messaging.{AddressableMessage, Message}
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.persistence.SaveSnapshotRequest

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

abstract class AggregateRootActorFactory[A <: AggregateRoot[_, _, A]: LocalOfficeId] extends BusinessEntityActorFactory[A] {
  def inactivityTimeout: Duration = 1.minute
}

trait AggregateState[S <: AggregateState[S]] {
  type StateMachine = PartialFunction[DomainEvent, S]
  def eventHandler: StateMachine
  def eventHandlerDefined(e: DomainEvent): Boolean = eventHandler.isDefinedAt(e)
  def initialized: Boolean                         = true
}

trait Uninitialized[S <: AggregateState[S]] { this: AggregateState[S] =>
  override def initialized = false
}

trait ReactionInterpreter {
  def execute(reaction: Reaction[_]): Unit
}

abstract class AggregateRoot[Event <: DomainEvent, S <: AggregateState[S]: Uninitialized, A <: AggregateRoot[Event, S, A]: LocalOfficeId]
    extends AggregateRootBase with CollaborationSupport[Event] with ReactionInterpreter {

  type HandlePayload = PartialFunction[Any, Reaction[_]]
  type HandleCommand = CommandHandlerContext[C] => PartialFunction[Command, Reaction[Event]]
  type HandleQuery   = PartialFunction[Query, Reaction[_]]

  def commandHandlerContext(cm: CommandMessage) = CommandHandlerContext(caseRef, config, cm.metadata)

  override def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
  override def department: String         = officeId.department

  private lazy val sm = new StateManager(onStateChanged = messageProcessed)

  def initialized: Boolean = state.initialized

  def state: S = sm.state

  override def preRestart(reason: Throwable, msgOpt: Option[Any]) {
    reply(Failure(reason))
    super.preRestart(reason, msgOpt)
  }

  override def receiveRecover: Receive = {
    case em: EventMessage =>
      sm(em)
    case Tagged(em @ EventMessage(_, _), _) =>
      sm(em)

    case SnapshotOffer(metadata, state) =>
      sm.reset(state.asInstanceOf[S])
      log.debug(s"Snapshot restored: $state")

    case RecoveryCompleted => // ignore
  }

  override def receiveCommand: Receive = {
    case msg: AddressableMessage =>
      safely {
        handlePayload(msg).orElse(handleUnknown).andThen(execute)(msg.payload)
      }

    case SaveSnapshotRequest =>
      val snapshot = state
      saveSnapshot(snapshot)

    case SaveSnapshotSuccess(metadata) =>
      log.debug("Snapshot saved successfully with metadata: {}", metadata)

    case f @ SaveSnapshotFailure(metadata, reason) =>
      log.error(s"$f")
      throw reason

  }

  private def handlePayload(msg: AddressableMessage): HandlePayload = {
    (if (isCommandMsgReceived) {
       handleCommand(commandHandlerContext(msg.asInstanceOf[CommandMessage]))
     } else {
       handleQuery
     }).asInstanceOf[HandlePayload]
  }

  def handleCommand: HandleCommand =
    state.asInstanceOf[Behavior[Event, S, C]].commandHandler

  private def handleQuery: HandleQuery =
    state.asInstanceOf[Behavior[Event, S, C]].qHandler

  private def handleUnknown: HandlePayload = {
    case payload: Any =>
      val payloadName = payload.getClass.getSimpleName
      Reject(
        if (initialized) {
          if (isCommandMsgReceived)
            new CommandHandlerNotDefined(payloadName)
          else
            new QueryHandlerNotDefined(payloadName)
        } else
          new AggregateRootNotInitialized(officeId.caseName, id, payloadName)
      )
  }

  override def execute(r: Reaction[_]): Unit =
    if (isCommandMsgReceived) {
      executeC(r.asInstanceOf[Reaction[Event]])
    } else {
      executeQ(r)
    }

  private def executeC(r: Reaction[Event]): Unit = r match {
    case AcceptC(events)  => raise(events)
    case c: Collaboration => c.execute(execute)
    case Reject(ex)       => reply(Failure(ex))
  }

  private def executeQ(r: Reaction[_]): Unit = r match {
    case AcceptQ(response) => msgSender ! response
    case Reject(ex)        => msgSender ! Failure(ex)
  }

  private def raise(events: Seq[Event]): Unit = {
    var eventsCount   = 0
    val eventMessages = events.map(e => toEventMessage(e, officeId, commandMsgReceived))

    val handler =
      sm.eventMessageHandler.andThen { _ =>
        eventsCount += 1
        if (eventsCount == events.size) {
          val oems = eventMessages.map(toOfficeEventMessage)
          handle(sender(), oems)
          reply(Success(oems))
        }
      }

    persistAll(eventMessages.toList)(e => safely(handler(e)))
  }

  private def reply(result: Try[Seq[OfficeEventMessage]], cm: CommandMessage = commandMsgReceived) {
    msgSender ! cm.deliveryReceipt(result.map(config.respondingPolicy.successMapper))
  }

  def handleDuplicated(msg: Message): Unit =
    reply(Success(Seq.empty), msg.asInstanceOf[CommandMessage])

  private def safely(f: => Unit): Unit =
    try f catch {
      case ex: Throwable => execute(new Reject(ex))
    }

  private class StateManager(onStateChanged: (EventMessage) => Unit) {
    private var s: S = implicitly[Uninitialized[S]].asInstanceOf[S]

    def state: S = s

    def reset(snapshot: S): Unit =
      s = snapshot

    def apply(em: EventMessage): Unit =
      apply(eventHandler, em)

    def eventMessageHandler: (EventMessage) => EventMessage = em => {
      apply(eventHandler, em)
      em
    }

    private def apply(eventHandler: Function[DomainEvent, S], em: EventMessage): Unit = {
      s = eventHandler(em.event)
      onStateChanged(em)
    }

    private def eventHandler: Function[DomainEvent, S] = event => {
      def eventName   = event.getClass.getSimpleName
      def commandName = commandMsgReceived.command.getClass.getSimpleName
      def caseName    = officeId.caseName
      s match {
        case state if state.eventHandlerDefined(event) =>
          state.eventHandler(event)
        case state if state.initialized =>
          throw new StateTransitionNotDefined(commandName, eventName)
        case _ =>
          throw new AggregateRootNotInitialized(caseName, id, commandName, Some(eventName))
      }
    }

  }

}
