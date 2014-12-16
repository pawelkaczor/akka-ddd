package pl.newicom.dddd.test.dummy

import akka.actor.Props
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{CreateDummy, DummyCreated, DummyUpdated, UpdateDummy}
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig.testSystem
import DummyOfficeSpec._
import scala.concurrent.duration.{Duration, _}

object DummyOfficeSpec {
  implicit def actorFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it
    }
}

class DummyOfficeSpec extends OfficeSpec[DummyAggregateRoot](testSystem) {

  def dummyOffice = officeUnderTest

  "Dummy office" should {
    "handle Create command" in {
      whenCommand(
        CreateDummy(aggregateId)
      )
      .expectEvent(
        DummyCreated(aggregateId)
      )
    }
  }

  "Dummy office" should {
    "handle Update command" in {
      givenCommand(
        CreateDummy(aggregateId)
      )
      .whenCommand(
        UpdateDummy(aggregateId)
      )
      .expectEvent(
        DummyUpdated(aggregateId)
      )
    }
  }
}
