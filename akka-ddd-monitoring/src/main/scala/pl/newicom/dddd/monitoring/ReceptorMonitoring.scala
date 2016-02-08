package pl.newicom.dddd.monitoring

import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.Inner
import pl.newicom.dddd.aggregate.{BusinessEntity, EntityId}
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.{DemandCallback, DemandConfig}
import pl.newicom.dddd.messaging.event.{EventMessageEntry, EventStreamSubscriber}

trait ReceptorMonitoring extends EventStreamSubscriber with TraceContextSupport {
  this: ReceivePipeline =>

  var observed: Option[EntityId] = None

  override abstract def subscribe(observable: BusinessEntity, fromPositionExclusive: Option[Long], demandConfig: DemandConfig): DemandCallback  = {
    observed = Some(observable.id)
    super.subscribe(observable, fromPositionExclusive, demandConfig)
  }


  pipelineOuter {
    case msg @ EventMessageEntry(em, _) =>
      val contextName  = s"${observed.getOrElse("???")}-${em.payloadName}"
      setNewCurrentTraceContext(contextName)
      Inner(msg)
  }


}
