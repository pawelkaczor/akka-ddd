package pl.newicom.dddd.process

trait ProcessManager[S <: SagaState[S]] extends Saga with SagaStateHandling[S]