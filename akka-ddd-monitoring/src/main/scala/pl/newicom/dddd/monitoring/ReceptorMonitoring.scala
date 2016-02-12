package pl.newicom.dddd.monitoring

import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.{EventSourceProvider, OfficeEventMessage}
import pl.newicom.dddd.monitoring.ReceptorMonitoring.Phase._

object ReceptorMonitoring {
  case class Phase(name: String) {
    def traceContextName(observed: BusinessEntity, msg: OfficeEventMessage): String =
      s"${observed.id}-${msg.payloadName}-$name"
  }

  object Phase {
    val Reaction = Phase("reaction")
    val Reception = Phase("reception")
  }
}

trait ReceptorMonitoring[ES] extends EventSourceProvider[ES] with TraceContextSupport {

  override abstract def eventSource(es: ES, observable: BusinessEntity, fromPosExcl: Option[Long]): EventSource =
    super.eventSource(es, observable, fromPosExcl) map {
      entry =>
        /**
          * Record elapsed time since the event was persisted in the event store
          */
        def recordCreationToReceptionPeriod() =
          newTraceContext(
            name            = Reception.traceContextName(observable, entry.msg),
            startedOnMillis = entry.created.get.getMillis
          ).foreach(
            _.finish()
          )

        def startRecordingReaction() =
          setNewCurrentTraceContext(
            name = Reaction.traceContextName(observable, entry.msg)
          )

        recordCreationToReceptionPeriod()
        startRecordingReaction()
        entry
    }

}
