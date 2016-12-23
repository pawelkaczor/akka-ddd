package pl.newicom.dddd.test.dummy

import akka.actor.Props
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.{AggregateRootActorFactory, EntityId}
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig.testSystem
import DummyOfficeSpec._
import pl.newicom.dddd.aggregate.error.AggregateRootNotInitializedException
import pl.newicom.dddd.office.Office

import scala.concurrent.duration.{Duration, _}

object DummyOfficeSpec {

  implicit def actorFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher {
        override def valueGenerator: Int = -1 // not allowed
      })
      override def inactivityTimeout: Duration = it

    }
}

class DummyOfficeSpec extends OfficeSpec[DummyAggregateRoot](Some(testSystem)) {

  def dummyOffice: Office = officeUnderTest

  def dummyId: EntityId = aggregateId

  "Dummy office" should {

    "create Dummy" in {
      when {
        CreateDummy(dummyId, "dummy name", "dummy description", 100)
      }
      .expect { c =>
        DummyCreated(c.id, c.name, c.description, c.value)
      }
    }

    "reject update of non-existing Dummy" in {
      when {
        ChangeName(dummyId, "some other dummy name")
      }
      .expectException[AggregateRootNotInitializedException]()
    }

    "reject CreateDummy if Dummy already exists" in {
      val dId = dummyId
      given {
        CreateDummy(dId, "dummy name", "dummy description", 100)
      }
      when {
        CreateDummy(dId, "dummy name", "dummy description", 100)
      }
      .expectException[RuntimeException]()
    }

    "update Dummy's name" in {
      given {
        CreateDummy(dummyId, "dummy name", "dummy description", 100)
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
        CreateDummy(dummyId, "dummy name", "dummy description", 100),
        ChangeName(dummyId, "some other dummy name")
      )
      .when {
        ChangeName(dummyId, "yet another dummy name")
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    "reject negative value" in {
      when {
        CreateDummy(dummyId, "dummy name", "dummy description", value = -1)
      }
      .expectException[RuntimeException]("negative value not allowed")
    }


    "reject negative generated value" in {
        given {
          CreateDummy(dummyId, "dummy name", "dummy description", 100)
        }
        .when {
          GenerateValue(dummyId)
        }
        .expectException[RuntimeException]("negative value not allowed")
    }

  }

}
