package pl.newicom.dddd.monitoring

import akka.actor.ActorRef
import akka.contrib.pattern.ReceivePipeline.Inner
import pl.newicom.dddd.aggregate.{AggregateRootBase, EventMessageFactory}
import pl.newicom.dddd.eventhandling.EventHandler
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.OfficeEventMessage
import pl.newicom.dddd.monitoring.Stage._

trait AggregateRootMonitoring extends EventHandler with EventMessageFactory with TraceContextSupport {
  this: AggregateRootBase =>

  override abstract def handle(senderRef: ActorRef, event: OfficeEventMessage): Unit = {
    super.handle(senderRef, event)
    finishCurrentTraceContext()
    log.debug("Event stored: {}", event.payload)
  }

  pipelineOuter {
    case cm: CommandMessage =>
      /**
        * Record elapsed time since the command was created (by write-front)
        */
      def recordCommandCreationToReceptionPeriod() =
        newTraceContext(
          name            = Reception_Of_Command.traceContextName(this, cm),
          startedOnMillis = cm.timestamp.getTime
        ).foreach(
          _.finish()
        )

      def startRecordingOfCommandHandling() =
        setNewCurrentTraceContext(
          name = Handling_Of_Command.traceContextName(this, cm)
        )

      log.debug("Received: {}", cm)

      recordCommandCreationToReceptionPeriod()
      startRecordingOfCommandHandling()

      Inner(cm)
  }


  def commandTraceContext = currentTraceContext
}
