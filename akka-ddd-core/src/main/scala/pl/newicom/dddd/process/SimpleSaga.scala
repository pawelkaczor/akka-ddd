package pl.newicom.dddd.process

import pl.newicom.dddd.aggregate.DomainEvent

trait SimpleSaga extends Saga with SagaAbstractStateHandling {

  def recoveryCompleted(): Unit = {
    // do nothing
  }

  def updateState(event: DomainEvent): Unit = {
    applyEvent.applyOrElse(event, (e: DomainEvent) => ())
  }

  def applyEvent: PartialFunction[DomainEvent, Unit]
}
