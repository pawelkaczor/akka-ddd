package pl.newicom.dddd.aggregate

import akka.actor.ActorRef
import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.Inner
import akka.persistence.PersistentActor
import pl.newicom.dddd.actor.{GracefulPassivation, PassivationConfig}
import pl.newicom.dddd.eventhandling.EventHandler
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.{EventMessage, OfficeEventMessage}
import pl.newicom.dddd.messaging.{AddressableMessage, Deduplication}
import pl.newicom.dddd.office.{CaseRef, OfficeId}
import pl.newicom.dddd.persistence.PersistentActorLogging

trait AggregateRootBase extends BusinessEntity with GracefulPassivation with PersistentActor
    with EventHandler with ReplyConfig with EventMessageFactory with ReceivePipeline with Deduplication with PersistentActorLogging {

  type C <: Config

  def state: Any
  def config: C
  override lazy val pc: PassivationConfig = config.pc

  override def id: EntityId = self.path.name

  def officeId: OfficeId

  override def persistenceId: String = officeId.caseRef(id).id

  def caseRef: CaseRef = CaseRef(persistenceId, officeId, Some(lastSequenceNr))

  /**
    * Sender of the currently processed command. Not available during recovery
    */
  def msgSender: ActorRef = _msgSender.get
  /**
    * Command being processed. Not available during recovery
    */
  def msgReceived: AddressableMessage = _msgReceived.get

  def commandMsgReceived: CommandMessage = _msgReceived.get.asInstanceOf[CommandMessage]

  def isCommandMsgReceived: Boolean = msgReceived.isInstanceOf[CommandMessage]

  private var _msgReceived: Option[AddressableMessage] = None

  // If an aggregate root actor collaborates with another actor while processing the command
  // (using CollaborationSupport trait), result of calling sender() after a message from collaborator
  // has been received will be a reference to the collaborator actor (instead of a reference to the command sender).
  // Thus we need to keep track of command sender as a variable.
  private var _msgSender: Option[ActorRef] = None

  pipelineOuter {
    case am: AddressableMessage =>
      _msgReceived = Some(am)
      _msgSender = Some(sender())
      Inner(am)
  }

  // Extension point
  override def handle(senderRef: ActorRef, events: Seq[OfficeEventMessage]): Unit = {}

  def toOfficeEventMessage(em: EventMessage) = OfficeEventMessage(em, caseRef)

}
