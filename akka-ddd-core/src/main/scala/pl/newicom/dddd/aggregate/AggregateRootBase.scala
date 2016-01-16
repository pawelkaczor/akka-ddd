package pl.newicom.dddd.aggregate

import akka.actor.ActorRef
import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.Inner
import akka.persistence.PersistentActor
import pl.newicom.dddd.actor.GracefulPassivation
import pl.newicom.dddd.eventhandling.EventHandler
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.{CaseId, EventMessage, OfficeEventMessage}
import pl.newicom.dddd.messaging.{CollaborationSupport, Deduplication, Message}
import pl.newicom.dddd.office.OfficeId
import pl.newicom.dddd.persistence.PersistentActorLogging

import scala.util.{Failure, Success, Try}


trait AggregateRootBase extends BusinessEntity with CollaborationSupport with GracefulPassivation with PersistentActor
    with EventHandler with ReceivePipeline with Deduplication with PersistentActorLogging {

  override def id = self.path.name

  def officeId: OfficeId

  override def persistenceId = officeId.clerkGlobalId(id)

  /**
    * Sender of the currently processed command. Not available during recovery
    */
  def currentCommandSender: ActorRef = _currentCommandSender.get
  /**
    * Command being processed. Not available during recovery
    */
  def currentCommandMessage: CommandMessage = _currentCommandMessage.get

  private var _currentCommandMessage: Option[CommandMessage] = None

  // If an aggregate root actor collaborates with another actor while processing the command
  // (using CollaborationSupport trait), result of calling sender() after a message from collaborator
  // has been received will be a reference to the collaborator actor (instead of a reference to the command sender).
  // Thus we need to keep track of command sender as a variable.
  private var _currentCommandSender: Option[ActorRef] = None

  override def preRestart(reason: Throwable, msgOpt: Option[Any]) {
    acknowledgeCommandProcessed(currentCommandMessage, Failure(reason))
    super.preRestart(reason, msgOpt)
  }

  pipelineOuter {
    case cm: CommandMessage =>
      log.debug(s"Received: $cm")
      _currentCommandMessage = Some(cm)
      _currentCommandSender = Some(sender())
      Inner(cm)
  }

  /**
    * Event handler, not invoked during recovery.
    */
  override def handle(senderRef: ActorRef, event: OfficeEventMessage) {
    acknowledgeCommandProcessed(currentCommandMessage)
  }

  def acknowledgeCommand(result: Any) =
    acknowledgeCommandProcessed(currentCommandMessage, Success(result))

  def acknowledgeCommandProcessed(msg: Message, result: Try[Any] = Success("OK")) {
    val deliveryReceipt = msg.deliveryReceipt(result)
    currentCommandSender ! deliveryReceipt
    log.debug(s"Delivery receipt (for received command) sent ($deliveryReceipt)")
  }

  def handleDuplicated(msg: Message) =
    acknowledgeCommandProcessed(msg)


  def toOfficeEventMessage(em: EventMessage) = OfficeEventMessage(em, CaseId(id, lastSequenceNr))

}
