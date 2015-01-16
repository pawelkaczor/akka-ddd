package pl.newicom.dddd.test.dummy

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
  case class ChangeDummyName(id: EntityId, name: String) extends Command
  case class ChangeDummyDescription(id: EntityId, description: String) extends Command
  case class ChangeDummyValue(id: EntityId, value: Any) extends Command

  //
  // Events
  //
  case class DummyCreated(id: EntityId, name: String, description: String, value: Any)
  case class DummyNameChanged(id: EntityId, name: String)
  case class DummyDescriptionChanged(id: EntityId, description: String)
  case class DummyValueChanged(id: EntityId, value: Any, dummyVersion: Long)


  case class DummyState(value: Any) extends AggregateState {
    override def apply = {
      case _ => this
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

    case ChangeDummyName(id, name) =>
      if (initialized)
        raise(DummyNameChanged(id, name))
      else
        throw new RuntimeException("Unknown Dummy")

    case ChangeDummyDescription(id, description) =>
      if (initialized)
        raise(DummyDescriptionChanged(id, description))
      else
        throw new RuntimeException("Unknown Dummy")

    case ChangeDummyValue(id, value) =>
      if (initialized) {
        if (value == null) {
          throw new RuntimeException("null value not allowed")
        } else {
          raise(DummyValueChanged(id, value, lastSequenceNr))
        }
      } else {
        throw new RuntimeException("Unknown Dummy")
      }
  }

  override val pc = PassivationConfig()
}