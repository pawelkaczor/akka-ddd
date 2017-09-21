package pl.newicom.dddd.monitoring

import akka.actor.ActorRef
import akka.contrib.pattern.ReceivePipeline.Inner
import kamon.trace.TraceContext
import pl.newicom.dddd.aggregate.{AggregateRootBase, EventMessageFactory}
import pl.newicom.dddd.eventhandling.EventHandler
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.OfficeEventMessage
import pl.newicom.dddd.monitoring.Stage._

trait AggregateRootMonitoring extends EventHandler with EventMessageFactory with TraceContextSupport {
  this: AggregateRootBase =>

  override abstract def handle(senderRef: ActorRef, events: Seq[OfficeEventMessage]): Unit = {
    super.handle(senderRef, events)
    finishCurrentTraceContext()
    log.debug("Events stored: {}", events.map(_.payload))
  }

  pipelineOuter {
    case cm: CommandMessage =>
      /**
        * Record elapsed time since the command was created (by write-front)
        */
      def recordCommandCreationToReceptionPeriod(): Unit =
        newTraceContext(
          name            = Reception_Of_Command.traceContextName(this, cm),
          startedOnMillis = cm.timestamp.getMillis
        ).foreach(
          _.finish()
        )

      def startRecordingOfCommandHandling(): Unit =
        setNewCurrentTraceContext(
          name = Handling_Of_Command.traceContextName(this, cm)
        )

      log.debug("Received: {}", cm)

      recordCommandCreationToReceptionPeriod()
      startRecordingOfCommandHandling()

      Inner(cm)
  }


  def commandTraceContext: TraceContext = currentTraceContext
}
