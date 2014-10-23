package pl.newicom.dddd.test

import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.{AggregateRoot, AggregateState, Command}
import pl.newicom.dddd.test.DummyAggregateRoot.{Create, Created}

object DummyAggregateRoot {

  // commands
  case class Create(name: String = "dummy") extends Command {
    override def aggregateId: String = name
  }

  // events
  case class Created(name: String = "dummy")
}

class DummyAggregateRoot extends AggregateRoot[DummyState] {

  override val factory: AggregateRootFactory = {
    case Created(name) => DummyState(name)
  }

  override def handleCommand: Receive = {
    case Create(name) => raise(Created(name))
  }

  override val passivationConfig: PassivationConfig = PassivationConfig()
}

case class DummyState(name: String) extends AggregateState {
  override def apply: StateMachine = throw new UnsupportedOperationException
}
