package pl.newicom.dddd.test.dummy

import akka.actor.Props
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._
import pl.newicom.dddd.test.dummy.DummyOfficeSpec._
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig.testSystem

import scala.concurrent.duration.{Duration, _}

object DummyOfficeSpec {
  implicit def actorFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it
    }
}

class DummyOfficeSpec extends OfficeSpec[DummyAggregateRoot] {

  def dummyOffice = officeUnderTest

  def dummyId = aggregateId

  "Dummy office" should {
    "create Dummy" in {
      when {
        CreateDummy(dummyId, "dummy name", "dummy description", "dummy value")
      }
      .expect { c =>
        DummyCreated(c.id, c.name, c.description, c.value)
      }
    }

    "update Dummy's name" in {
      given {
        CreateDummy(dummyId, "dummy name", "dummy description", "dummy value")
      }
      .when {
        ChangeName(dummyId, "some other dummy name")
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    "handle subsequent Update command" in {
      given(
        CreateDummy(dummyId, "dummy name", "dummy description", "dummy value"),
        ChangeName(dummyId, "some other dummy name")
      )
      .when {
        ChangeName(dummyId, "yet another dummy name")
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    "reject null value" in {
      when {
        CreateDummy(dummyId, "dummy name", "dummy description", value = null)
      }
      .expectException[RuntimeException]("null value not allowed")
    }

  }

}
