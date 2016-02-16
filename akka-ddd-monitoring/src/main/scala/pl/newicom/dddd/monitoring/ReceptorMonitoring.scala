package pl.newicom.dddd.monitoring

import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.EventSourceProvider
import pl.newicom.dddd.monitoring.Stage._

trait ReceptorMonitoring[ES] extends EventSourceProvider[ES] with TraceContextSupport {

  override abstract def eventSource(es: ES, observable: BusinessEntity, fromPosExcl: Option[Long]): EventSource =
    super.eventSource(es, observable, fromPosExcl) map {
      entry =>
        /**
          * Record elapsed time since the event was persisted in the event store
          */
        def recordCreationToReceptionPeriod() =
          newTraceContext(
            name            = Reception_Of_Event.traceContextName(observable, entry.msg),
            startedOnMillis = entry.created.get.getMillis
          ).foreach(
            _.finish()
          )

        recordCreationToReceptionPeriod()
        entry.copy(msg = entry.msg.withMetaAttribute(Reaction_On_Event.shortName, System.nanoTime()))
    }

}
