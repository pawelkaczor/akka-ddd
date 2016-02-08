package pl.newicom.dddd.monitoring

import kamon.trace.Tracer
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.process.{Saga, SagaAbstractStateHandling}

trait SagaMonitoring extends SagaAbstractStateHandling {
  this: Saga =>

  override abstract def updateState(event: DomainEvent): Unit = {
    super.updateState(event)
    Option(Tracer.currentContext).foreach(_.finish())
  }

}
