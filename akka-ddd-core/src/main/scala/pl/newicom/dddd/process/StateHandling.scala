package pl.newicom.dddd.process

import pl.newicom.dddd.aggregate.DomainEvent

trait StateHandling[S <: SagaState[S]] extends SagaAbstractStateHandling {

  type StateFunction = PartialFunction[DomainEvent, S]
  type StateMachine  = PartialFunction[S, StateFunction]

  private var stateOpt: Option[S] = None

  def initialState: S

  def stateMachine: StateMachine

  def updateState(event: DomainEvent): Unit = {
    stateOpt = Some(stateMachine.apply(state).applyOrElse(event, stay))
  }

  def state: S = stateOpt.getOrElse(initialState)

  def stay: StateFunction = {
    case _ => state
  }

}
