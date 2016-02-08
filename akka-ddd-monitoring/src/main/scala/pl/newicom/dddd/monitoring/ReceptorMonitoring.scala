package pl.newicom.dddd.monitoring

import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.Inner
import kamon.Kamon
import kamon.trace.Tracer
import pl.newicom.dddd.aggregate.{EntityId, BusinessEntity}
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.{DemandCallback, DemandConfig}
import pl.newicom.dddd.messaging.event.{EventMessageEntry, EventStreamSubscriber}

trait ReceptorMonitoring extends EventStreamSubscriber {
  this: ReceivePipeline =>

  var observed: Option[EntityId] = None

  override abstract def subscribe(observable: BusinessEntity, fromPositionExclusive: Option[Long], demandConfig: DemandConfig): DemandCallback  = {
    observed = Some(observable.id)
    super.subscribe(observable, fromPositionExclusive, demandConfig)
  }


  pipelineOuter {
    case msg @ EventMessageEntry(em, _) =>
      try {
        val contextName  = s"${em.payload.getClass.getSimpleName} from ${observed.getOrElse("???")}"
        Tracer.setCurrentContext(Kamon.tracer.newContext(contextName))
      } catch {
        case e: NoClassDefFoundError => // Kamon not initialized, ignore
        case e: ExceptionInInitializerError => // Kamon not initialized, ignore
      }
      Inner(msg)
  }


}
