package pl.newicom.dddd.process

import akka.actor.ActorLogging
import pl.newicom.dddd.aggregate.DomainEvent

trait SagaState[T <: SagaState[T]]

trait SagaStateHandling[S <: SagaState[S]] extends SagaAbstractStateHandling {
  this: ActorLogging =>

  type StateFunction = PartialFunction[DomainEvent, S]
  type StateMachine  = PartialFunction[S, StateFunction]

  private var currentState: S = _
  private var currentEvent: DomainEvent = _
  private var initiation: StateFunction = _
  private var stateMachine: StateMachine = _

  def state = Option(currentState).getOrElse(initiation(currentEvent))

  class SagaBuilder(init: StateFunction) {
    def andThen(sm: StateMachine)  = {
      initiation = init
      stateMachine = sm.orElse({case s => initiation})
    }
  }

  def startWhen(initiation: StateFunction) = new SagaBuilder(initiation)

  def receiveEvent: ReceiveEvent = {
    case e: DomainEvent if stateMachine(currentState).isDefinedAt(e) =>
      RaiseEvent(e)
    case _ =>
      RejectEvent
  }

  def updateState(event: DomainEvent): Unit = {
    currentEvent = event
    def stay: StateFunction = {
      case _ => currentState
    }

    val oldState = currentState
    currentState = stateMachine(state).applyOrElse(event, stay)

    onTransition(Option(oldState))
  }

  def onTransition(oldState: Option[S]): Unit = {
    if (oldState.isDefined) {
      log.debug(s"State transition occurred: ${oldState.get} -> $currentState")
    } else {
      log.debug(s"Initial state: $currentState")
    }
  }

  //
  // Simple DSL
  //

  def stay(): S = currentState

  def goto(nextState: S): S = nextState

}