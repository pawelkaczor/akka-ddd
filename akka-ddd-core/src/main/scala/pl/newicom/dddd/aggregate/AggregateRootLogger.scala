package pl.newicom.dddd.aggregate

import akka.actor.ActorRef
import akka.contrib.pattern.ReceivePipeline.Inner
import pl.newicom.dddd.aggregate.AggregateRootSupport.{AcceptC, AcceptQ, Reaction, Reject}
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.OfficeEventMessage
import pl.newicom.dddd.messaging.query.QueryMessage

trait AggregateRootLogger[E <: DomainEvent] extends AggregateRootBase with CollaborationSupport[E] with ReactionInterpreter {
  this: AggregateRoot[E, _, _] =>

  pipelineOuter {
    case msg =>
      msg match {
        case cm: CommandMessage =>
          log.debug(s"Command received: ${cm.command }")
        case qm: QueryMessage =>
          log.debug(s"Query received: ${qm.query}")
        case _ =>
          log.debug(s"Message received: $msg")
      }
      Inner(msg)
  }

  abstract override def execute(reaction: Reaction[_]): Unit = {
    super.execute(reaction)
    if (isCommandMsgReceived) {
      reaction match {
        case c: Collaboration =>
          log.debug(s"Message ${c.msg } sent to ${c.target.path.name }")

        case AcceptC(events) if events.size == 1 =>
          log.debug(s"Command accepted. ${events.head }")

        case AcceptC(events) =>
          log.debug(s"Command accepted. $events")

        case Reject(ex) =>
          log.debug(s"Command rejected. $ex")
      }
    } else reaction match {

      case AcceptQ(response) =>
        log.debug(s"Query accepted. $response")

      case Reject(ex) =>
        log.debug(s"Query rejected. $ex")
    }
  }


  override abstract def handle(senderRef: ActorRef, events: Seq[OfficeEventMessage]): Unit = {
    super.handle(senderRef, events)
    log.debug(s"State: $state")
  }
}