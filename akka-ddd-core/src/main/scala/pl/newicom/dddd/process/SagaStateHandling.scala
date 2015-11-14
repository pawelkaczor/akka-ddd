package pl.newicom.dddd.process

import pl.newicom.dddd.aggregate.DomainEvent

trait SagaState[T <: SagaState[T]]

trait SagaStateHandling[S <: SagaState[S]] extends SagaAbstractStateHandling {

  type StateFunction = PartialFunction[DomainEvent, S]
  type StateMachine = PartialFunction[S, StateFunction]

  private var stateOpt: Option[S] = None

  def initialState: S

  def state: S = stateOpt.getOrElse(initialState)

  def stateMachine: StateMachine

  def receiveEvent: ReceiveEvent = {
    case e: DomainEvent if stateMachine.apply(state).isDefinedAt(e) =>
      ProcessEvent
    case _ =>
      RejectEvent
  }

  def updateState(event: DomainEvent): Unit = {
    stateOpt = Some(stateMachine(state).applyOrElse(event, stay))
  }

  def stay: StateFunction = {
    case _ => state
  }

}