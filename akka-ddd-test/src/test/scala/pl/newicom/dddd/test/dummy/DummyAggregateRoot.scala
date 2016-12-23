package pl.newicom.dddd.test.dummy

import java.util.UUID

import akka.actor.ActorRef
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate
import pl.newicom.dddd.aggregate.{AggregateRoot, AggregateState, DomainEvent, EntityId}
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._
import pl.newicom.dddd.test.dummy.ValueGeneratorActor.GenerateRandom
import pl.newicom.dddd.utils.UUIDSupport.uuidObj

import scala.concurrent.duration._

object DummyAggregateRoot {

  //
  // Commands
  //
  sealed trait Command extends aggregate.Command {
    def id: EntityId
    override def aggregateId: String = id
  }
  sealed trait UpdateCommand extends Command

  case class CreateDummy(id: EntityId, name: String, description: String, value: Int) extends Command

  case class ChangeName(id: EntityId, name: String)                                   extends UpdateCommand
  case class ChangeDescription(id: EntityId, description: String)                     extends UpdateCommand
  case class ChangeValue(id: EntityId, value: Int)                                    extends UpdateCommand
  case class GenerateValue(id: EntityId)                                              extends UpdateCommand
  case class ConfirmGeneratedValue(id: EntityId, confirmationToken: UUID)             extends UpdateCommand

  //
  // Events
  //
  sealed trait DummyEvent {
    def id: EntityId
  }
  case class DummyCreated(id: EntityId, name: String, description: String, value: Int) extends DummyEvent
  case class NameChanged(id: EntityId, name: String)                                   extends DummyEvent
  case class DescriptionChanged(id: EntityId, description: String)                     extends DummyEvent
  case class ValueChanged(id: EntityId, value: Int, dummyVersion: Long)                extends DummyEvent
  case class ValueGenerated(id: EntityId, value: Int, confirmationToken: UUID)         extends DummyEvent

  case class CandidateValue(value: Int, confirmationToken: UUID)

  case class DummyState(value: Int, candidateValue: Option[CandidateValue] = None) extends AggregateState[DummyState] {
    override def apply: PartialFunction[DomainEvent, DummyState] = {
      case ValueChanged(_, newValue, _) =>
        copy(value = newValue, candidateValue = None)
      case ValueGenerated(_, newValue, confirmationToken) =>
        copy(candidateValue = Some(CandidateValue(newValue, confirmationToken)))
      case _: NameChanged => this
      case _: DescriptionChanged => this
    }

    def candidateValue(confirmationToken: UUID): Option[Int] = {
      candidateValue.flatMap { cv =>
        if (cv.confirmationToken == confirmationToken) Some(cv.value) else None
      }
    }
  }

}

class DummyAggregateRoot extends AggregateRoot[DummyState, DummyAggregateRoot] {
  this: EventPublisher =>

  def valueGenerator: Int = (Math.random() * 100).toInt

  val valueGeneratorActor: ActorRef = context.actorOf(ValueGeneratorActor.props(valueGenerator))

  override val factory: AggregateRootFactory = {
    case DummyAggregateRoot.DummyCreated(_, _, _, value) => DummyState(value)
  }

  override def handleCommand: Receive = {

    case CreateDummy(id, name, description, value) =>
      if (value < 0) {
        sys.error("negative value not allowed")
      } else {
        raise(DummyCreated(id, name, description, value))
      }

    case ChangeName(id, name) =>
      raise(NameChanged(id, name))

    case ChangeDescription(id, description) =>
      raise(DescriptionChanged(id, description))

    case ChangeValue(id, value) =>
      if (value < 0) {
        sys.error("negative value not allowed")
      } else {
        raise(ValueChanged(id, value, lastSequenceNr))
      }

    case GenerateValue(id) =>
      implicit val timeout = 3.seconds
      (valueGeneratorActor !< GenerateRandom) {
        case ValueGeneratorActor.ValueGenerated(value) =>
          if (value < 0) {
            sys.error("negative value not allowed")
          } else {
            raise(ValueGenerated(id, value, confirmationToken = generateConfirmationToken))
          }
      }

    case ConfirmGeneratedValue(id, confirmationToken) =>
      state.candidateValue(confirmationToken).foreach { value =>
        raise(ValueChanged(id, value, lastSequenceNr))
      }
  }

  def generateConfirmationToken: UUID = uuidObj

  override val pc = PassivationConfig()
}