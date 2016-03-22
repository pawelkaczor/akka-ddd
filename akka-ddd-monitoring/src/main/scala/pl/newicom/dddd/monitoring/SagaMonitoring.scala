package pl.newicom.dddd.monitoring

import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.monitoring.Stage._
import pl.newicom.dddd.process.{Saga, SagaAbstractStateHandling}

trait SagaMonitoring extends SagaAbstractStateHandling with TraceContextSupport {
  this: Saga =>

  override abstract def updateState(event: DomainEvent): Unit = {
    super.updateState(event)
    val reactionOnEvent: Option[Long] = currentEventMsg.tryGetMetaAttribute(Reaction_On_Event.shortName)

    if (!recoveryRunning && reactionOnEvent.isDefined) {
      // finish 'reaction' record
      newLocalTraceContext(
        name            = Reaction_On_Event.traceContextName(officeId, currentEventMsg),
        startedOnNanos = reactionOnEvent.get
      ).foreach(
        _.finish()
      )
    }
  }

}
