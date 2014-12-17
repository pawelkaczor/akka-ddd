package pl.newicom.dddd.test.dummy

import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate
import pl.newicom.dddd.aggregate.{AggregateRoot, AggregateState, EntityId}
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._

object DummyAggregateRoot {

  sealed trait Command extends aggregate.Command {
    def name: EntityId
    override def aggregateId: String = name
  }
  // commands
  case class CreateDummy(name: EntityId = "dummy") extends Command

  case class UpdateDummy(name: EntityId = "dummy") extends Command

  // events
  case class DummyCreated(name: String = "dummy")
  case class DummyUpdated(name: String = "dummy")

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
    case DummyAggregateRoot.DummyCreated(name) => DummyState(name)
  }

  override def handleCommand: Receive = {
    case CreateDummy(name) => raise(DummyCreated(name))
    case UpdateDummy(name) => raise(DummyUpdated(name))
  }

  override val pc: PassivationConfig = PassivationConfig()
}