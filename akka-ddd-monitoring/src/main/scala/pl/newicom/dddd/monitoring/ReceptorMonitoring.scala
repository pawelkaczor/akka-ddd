package pl.newicom.dddd.monitoring

import akka.actor.Actor
import pl.newicom.dddd.BusinessEntity
import pl.newicom.dddd.messaging.event.EventSourceProvider
import pl.newicom.dddd.monitoring.Stage._

trait ReceptorMonitoring extends EventSourceProvider with TraceContextSupport {
  this: Actor =>

  override abstract def eventSource(es: EventStore, observable: BusinessEntity, fromPosExcl: Option[Long]): EventSource =
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
