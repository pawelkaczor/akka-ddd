package pl.newicom.dddd.aggregate

import akka.actor.Status.Failure
import akka.actor._
import akka.persistence._
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, GracefulPassivation, PassivationConfig}
import pl.newicom.dddd.aggregate.error.AggregateRootNotInitializedException
import pl.newicom.dddd.delivery.protocol.Acknowledged
import pl.newicom.dddd.eventhandling.EventHandler
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.{AggregateSnapshotId, DomainEventMessage, EventMessage}

import scala.concurrent.duration.{Duration, _}

trait AggregateState {
  type StateMachine = PartialFunction[DomainEvent, AggregateState]
  def apply: StateMachine
}

abstract class AggregateRootActorFactory[A <: AggregateRoot[_]] extends BusinessEntityActorFactory[A] {
  def props(pc: PassivationConfig): Props
  def inactivityTimeout: Duration = 1.minute
}

trait AggregateRoot[S <: AggregateState]
  extends BusinessEntity with GracefulPassivation with PersistentActor with EventHandler with ActorLogging {

  type AggregateRootFactory = PartialFunction[DomainEvent, S]
  private var stateOpt: Option[S] = None
  private var _lastCommandMessage: Option[CommandMessage] = None
  val factory: AggregateRootFactory

  override def persistenceId: String = id
  override def id = self.path.name

  override def receiveCommand: Receive = {
    case cm: CommandMessage =>
      log.debug(s"Received: $cm")
      _lastCommandMessage = Some(cm)
      handleCommand.applyOrElse(cm.command, unhandled)
  }

  override def receiveRecover: Receive = {
    case event: EventMessage =>
      updateState(event.event)
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    sender() ! Failure(reason)
    super.preRestart(reason, message)
  }

  def commandMessage = _lastCommandMessage.get

  def handleCommand: Receive

  def updateState(event: DomainEvent) {
    val nextState = if (initialized) state.apply(event) else factory.apply(event)
    stateOpt = Option(nextState.asInstanceOf[S])
  }

  def raise(event: DomainEvent) {
    persist(new EventMessage(event = event).withMetaData[EventMessage](commandMessage.metadata)) {
      persisted =>
        {
          log.info("Event persisted: {}", event)
          updateState(event)
          handle(toDomainEventMessage(persisted))
        }
    }
  }

  def toDomainEventMessage(persisted: EventMessage) =
    new DomainEventMessage(persisted, AggregateSnapshotId(id, lastSequenceNr)).withMetaData[DomainEventMessage](persisted.metadata)

  override def handle(event: DomainEventMessage) {
    sender ! Acknowledged
  }

  def initialized = stateOpt.isDefined

  def state = if (initialized) stateOpt.get else throw new AggregateRootNotInitializedException

}
