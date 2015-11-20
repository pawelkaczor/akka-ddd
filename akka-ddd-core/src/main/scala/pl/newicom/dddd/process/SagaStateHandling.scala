package pl.newicom.dddd.process

import akka.actor.ActorLogging
import akka.persistence.PersistentActor
import pl.newicom.dddd.aggregate.DomainEvent

trait SagaState[T <: SagaState[T]]

trait SagaStateHandling[S <: SagaState[S]] extends SagaAbstractStateHandling {
  this: PersistentActor with ActorLogging =>

  type StateFunction = PartialFunction[DomainEvent, S]
  type StateMachine  = PartialFunction[S, StateFunction]

  private var currentState: S = _
  private var initiation: StateFunction = _
  private var stateMachine: StateMachine = _

  def state = currentState

  class SagaBuilder(init: StateFunction) {
    def andThen(sm: StateMachine)  = {
      initiation = init
      stateMachine = sm
    }
  }

  def startWhen(initiation: StateFunction) = new SagaBuilder(initiation)

  def receiveEvent: ReceiveEvent = {
    case e: DomainEvent if canHandle(e) =>
      RaiseEvent(e)
    case _ =>
      RejectEvent
  }

  def updateState(event: DomainEvent): Unit = {
    val oldState = Option(currentState)
    val inputState = oldState.getOrElse(initiation(event))

    def default: PartialFunction[DomainEvent, S] = {case _ => inputState} // accept delivery receipt
    currentState = stateMachine.applyOrElse[S, StateFunction](inputState, {case _ => default}).applyOrElse(event, default)

    onEventApplied(event, inputState)
  }

  def onEventApplied(event: DomainEvent, oldState: S): Unit = {
    log.debug(s"Event applied: $oldState -> $currentState")
  }

  private def canHandle(event: DomainEvent): Boolean = {
    val fromStatePF = stateMachine.orElse[S, StateFunction]({case null => initiation})
    if (fromStatePF.isDefinedAt(currentState)) {
      fromStatePF(currentState).isDefinedAt(event)
    } else {
      log.debug(s"No transition found from state $currentState for event $event")
      false
    }

  }

  //
  // Simple DSL
  //

  def stay(): S = currentState

  def goto(nextState: S): S = nextState

}