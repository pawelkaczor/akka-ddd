package pl.newicom.dddd.process

import akka.actor.ActorLogging
import pl.newicom.dddd.aggregate.DomainEvent

trait SagaState[T <: SagaState[T]]

trait SagaStateHandling[S <: SagaState[S]] extends SagaAbstractStateHandling {
  this: ActorLogging =>

  type StateFunction = PartialFunction[DomainEvent, S]
  type StateMachine  = PartialFunction[S, StateFunction]

  private var currentState: S = _
  private var stateMachine: StateMachine = _

  def state = currentState

  def startWith(initialState: S)(sm: StateMachine): Unit = {
    stateMachine = sm
    currentState = initialState
    log.debug(s"Saga initialized with state: $currentState")
  }

  def receiveEvent: ReceiveEvent = {
    case e: DomainEvent if stateMachine(currentState).isDefinedAt(e) =>
      RaiseEvent(e)
    case _ =>
      RejectEvent
  }

  def updateState(event: DomainEvent): Unit = {
    def stay: StateFunction = {
      case _ => currentState
    }

    val oldState = currentState
    currentState = stateMachine(currentState).applyOrElse(event, stay)

    onTransition(oldState)
  }

  def onTransition(oldState: S): Unit = {
    log.debug(s"State transition occurred: $oldState -> $currentState")
  }

  //
  // Simple DSL
  //

  def stay(): S = currentState

  def goto(nextState: S): S = nextState

}