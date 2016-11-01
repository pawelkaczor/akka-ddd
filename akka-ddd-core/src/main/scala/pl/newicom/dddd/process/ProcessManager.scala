package pl.newicom.dddd.process

abstract class ProcessManager[S <: SagaState[S]] extends Saga with SagaCollaboration with SagaStateHandling[S] {
  def processManagerId = sagaId
}