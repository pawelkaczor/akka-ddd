package pl.newicom.dddd.monitoring

import akka.actor.ActorRef
import akka.contrib.pattern.ReceivePipeline.Inner
import kamon.Kamon
import kamon.trace.Tracer
import pl.newicom.dddd.aggregate.AggregateRootBase
import pl.newicom.dddd.eventhandling.EventHandler
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.OfficeEventMessage

trait AggregateRootMonitoring extends EventHandler {
  this: AggregateRootBase =>

  override abstract def handle(senderRef: ActorRef, event: OfficeEventMessage): Unit = {
    super.handle(senderRef, event)
    Tracer.currentContext.finish()
    log.debug("Event stored: {}", event.payload)
  }


  pipelineOuter {
    case cm: CommandMessage =>
      log.debug("Received: {}", cm)
      Tracer.setCurrentContext(Kamon.tracer.newContext("CommandMessage"))
      Inner(cm)
  }


}
