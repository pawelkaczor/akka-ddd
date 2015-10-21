package pl.newicom.dddd.test.dummy

import java.util.UUID

import akka.actor.Props
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate
import pl.newicom.dddd.aggregate.{AggregateRoot, AggregateState, EntityId}
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.messaging.CollaborationSupport
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._
import pl.newicom.dddd.test.dummy.ValueGenerator.GenerateRandom
import pl.newicom.dddd.utils.UUIDSupport.uuidObj

object DummyAggregateRoot {

  //
  // Commands
  //
  sealed trait Command extends aggregate.Command {
    def id: EntityId
    override def aggregateId: String = id
  }

  case class CreateDummy(id: EntityId, name: String, description: String, value: Int) extends Command
  case class ChangeName(id: EntityId, name: String) extends Command
  case class ChangeDescription(id: EntityId, description: String) extends Command
  case class ChangeValue(id: EntityId, value: Int) extends Command
  case class GenerateValue(id: EntityId) extends Command
  case class ConfirmGeneratedValue(id: EntityId, confirmationToken: UUID) extends Command

  //
  // Events
  //
  sealed trait DummyEvent {
    def id: EntityId
  }
  case class DummyCreated(id: EntityId, name: String, description: String, value: Int) extends DummyEvent
  case class NameChanged(id: EntityId, name: String) extends DummyEvent
  case class DescriptionChanged(id: EntityId, description: String) extends DummyEvent
  case class ValueChanged(id: EntityId, value: Int, dummyVersion: Long) extends DummyEvent
  case class ValueGenerated(id: EntityId, value: Int, confirmationToken: UUID) extends DummyEvent

  case class CandidateValue(value: Int, confirmationToken: UUID)

  case class DummyState(value: Int, candidateValue: Option[CandidateValue] = None) extends AggregateState {
    override def apply = {
      case ValueChanged(_, newValue, _) =>
        copy(value = newValue, candidateValue = None)
      case ValueGenerated(_, newValue, confirmationToken) =>
        copy(candidateValue = Some(CandidateValue(newValue, confirmationToken)))
      case _ => this
    }

    def candidateValue(confirmationToken: UUID): Option[Int] = {
      candidateValue.flatMap { cv =>
        if (cv.confirmationToken == confirmationToken) Some(cv.value) else None
      }
    }
  }

}

class DummyAggregateRoot extends CollaborationSupport with AggregateRoot[DummyState] {
  this: EventPublisher =>

  val valueGenerator = context.actorOf(Props[ValueGenerator])

  override def persistenceId = s"${dummyOffice.name}-$id"

  override val factory: AggregateRootFactory = {
    case DummyAggregateRoot.DummyCreated(_, _, _, value) => DummyState(value)
  }

  override def handleCommand: Receive = {

    case CreateDummy(id, name, description, value) =>
      if (initialized) {
        throw new RuntimeException("Dummy already exists")
      } else {
        if (value < 0) {
          throw new RuntimeException("negative value not allowed")
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
        if (value < 0) {
          throw new RuntimeException("negative value not allowed")
        } else {
          raise(ValueChanged(id, value, lastSequenceNr))
        }
      } else {
        throw new RuntimeException("Unknown Dummy")
      }

    case GenerateValue(id) =>
      if (initialized) {
        receiveNext {
          case ValueGenerator.ValueGenerated(value) =>
            raise(ValueGenerated(id, value, confirmationToken = uuidObj))
        }
        valueGenerator ! GenerateRandom
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