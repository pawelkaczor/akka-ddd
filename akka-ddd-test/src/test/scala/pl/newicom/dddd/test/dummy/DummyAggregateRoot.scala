package pl.newicom.dddd.test.dummy

import java.util.UUID

import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate
import pl.newicom.dddd.aggregate.{AggregateRoot, AggregateState, EntityId}
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._

object DummyAggregateRoot {

  //
  // Commands
  //
  sealed trait Command extends aggregate.Command {
    def id: EntityId
    override def aggregateId: String = id
  }

  case class CreateDummy(id: EntityId, name: String, description: String, value: Any) extends Command
  case class ChangeName(id: EntityId, name: String) extends Command
  case class ChangeDescription(id: EntityId, description: String) extends Command
  case class ChangeValue(id: EntityId, value: Any) extends Command
  case class GenerateValue(id: EntityId) extends Command
  case class ConfirmGeneratedValue(id: EntityId, confirmationToken: UUID) extends Command

  //
  // Events
  //
  case class DummyCreated(id: EntityId, name: String, description: String, value: Any)
  case class NameChanged(id: EntityId, name: String)
  case class DescriptionChanged(id: EntityId, description: String)
  case class ValueChanged(id: EntityId, value: Any, dummyVersion: Long)
  case class ValueGenerated(id: EntityId, value: Any, confirmationToken: UUID)

  case class CandidateValue(value: Any, confirmationToken: UUID)

  case class DummyState(value: Any, candidateValue: Option[CandidateValue] = None) extends AggregateState {
    override def apply = {
      case ValueChanged(_, newValue, _) =>
        copy(value = newValue, candidateValue = None)
      case ValueGenerated(_, newValue, confirmationToken) =>
        copy(candidateValue = Some(CandidateValue(newValue, confirmationToken)))
      case _ => this
    }

    def candidateValue(confirmationToken: UUID): Option[Any] = {
      candidateValue.flatMap { cv =>
        if (cv.confirmationToken == confirmationToken) Some(cv.value) else None
      }
    }
  }

}

class DummyAggregateRoot extends AggregateRoot[DummyState] {
  this: EventPublisher =>

  override def persistenceId = "Dummy-" + id

  override val factory: AggregateRootFactory = {
    case DummyAggregateRoot.DummyCreated(_, _, _, value) => DummyState(value)
  }

  override def handleCommand: Receive = {

    case CreateDummy(id, name, description, value) =>
      if (initialized) {
        throw new RuntimeException("Dummy already exists")
      } else {
        if (value == null) {
          throw new RuntimeException("null value not allowed")
        } else {
          raise(DummyCreated(id, name, description, value))
        }
      }

    case ChangeName(id, name) =>
      if (initialized)
        raise(NameChanged(id, name))
      else
        throw new RuntimeException("Unknown Dummy")

    case ChangeDescription(id, description) =>
      if (initialized)
        raise(DescriptionChanged(id, description))
      else
        throw new RuntimeException("Unknown Dummy")

    case ChangeValue(id, value) =>
      if (initialized) {
        if (value == null) {
          throw new RuntimeException("null value not allowed")
        } else {
          raise(ValueChanged(id, value, lastSequenceNr))
        }
      } else {
        throw new RuntimeException("Unknown Dummy")
      }

    case GenerateValue(id) =>
      if (initialized) {
        raise(ValueGenerated(id, value = UUID.randomUUID().toString, confirmationToken = UUID.randomUUID()))
      } else {
        throw new RuntimeException("Unknown Dummy")
      }

    case ConfirmGeneratedValue(id, confirmationToken) =>
      if (initialized) {
        state.candidateValue(confirmationToken).foreach { value =>
          raise(ValueChanged(id, value, lastSequenceNr))
        }
      } else {
        throw new RuntimeException("Unknown Dummy")
      }

  }

  override val pc = PassivationConfig()
}