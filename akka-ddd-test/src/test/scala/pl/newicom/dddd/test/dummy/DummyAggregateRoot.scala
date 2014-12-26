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
    def name: EntityId
    override def aggregateId: String = name
  }

  case class CreateDummy(name: EntityId) extends Command
  case class UpdateDummy(name: EntityId) extends Command
  case class InvalidUpdateDummy(name: EntityId) extends Command

  //
  // Events
  //
  case class DummyCreated(name: String, version: Long)
  case class DummyUpdated(name: String, version: Long)


  case class DummyState(name: String) extends AggregateState {
    override def apply = {
      case _ => this
    }
  }

}

class DummyAggregateRoot extends AggregateRoot[DummyState] {
  this: EventPublisher =>

  override def persistenceId = "Dummy-" + id

  override val factory: AggregateRootFactory = {
    case DummyAggregateRoot.DummyCreated(name, _) => DummyState(name)
  }

  override def handleCommand: Receive = {
    case CreateDummy(name) =>
      raise(DummyCreated(name, lastSequenceNr))
    case UpdateDummy(name) =>
      raise(DummyUpdated(name, lastSequenceNr))
    case InvalidUpdateDummy(name) =>
      throw new RuntimeException("Update rejected")
  }

  override val pc = PassivationConfig()
}