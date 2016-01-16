package pl.newicom.dddd.process

abstract class ProcessManager[S <: SagaState[S]] extends Saga with SagaStateHandling[S]