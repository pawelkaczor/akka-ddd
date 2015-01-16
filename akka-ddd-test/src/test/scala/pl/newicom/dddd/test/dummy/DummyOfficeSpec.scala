package pl.newicom.dddd.test.dummy

import akka.actor.Props
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._
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

  def dummyId = aggregateId

  "Dummy office" should {
    "create Dummy" in {
      whenCommand {
        CreateDummy(dummyId, "dummy name", "dummy description", "dummy value")
      }
      .expectEvent2 { c =>
        DummyCreated(dummyId, c.name, c.description, c.value)
      }
    }
  }

  "Dummy office" should {
    "update Dummy's name" in {
      givenCommand {
        CreateDummy(dummyId, "dummy name", "dummy description", "dummy value")
      }
      .whenCommand {
        ChangeDummyName(dummyId, "some other dummy name")
      }
      .expectEvent2 { c =>
        DummyNameChanged(dummyId, c.name)
      }
    }
  }

  "Dummy office" should {
    "handle subsequent Update command" in {
      givenCommands(
        CreateDummy(dummyId, "dummy name", "dummy description", "dummy value"),
        ChangeDummyName(dummyId, "some other dummy name")
      )
      .whenCommand {
        ChangeDummyName(dummyId, "yet another dummy name")
      }
      .expectEvent2 { c =>
        DummyNameChanged(dummyId, c.name)
      }
    }
  }

  "Dummy office" should {
    "reject null value" in {
      whenCommand {
        CreateDummy(dummyId, "dummy name", "dummy description", value = null)
      }
      .expectException[RuntimeException]("null value not allowed")
    }
  }

}
