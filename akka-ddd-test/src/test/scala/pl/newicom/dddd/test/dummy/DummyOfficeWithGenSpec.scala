package pl.newicom.dddd.test.dummy

import akka.actor.Props
import org.scalacheck.Gen
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{DummyNameChanged, ChangeDummyName, DummyCreated, CreateDummy}
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig._
import DummyOfficeWithGenSpec._

import scala.concurrent.duration.{Duration, _}

object DummyOfficeWithGenSpec {
  implicit def actorFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it
    }
}

class DummyOfficeWithGenSpec extends OfficeSpec[DummyAggregateRoot](testSystem) {

  def dummyOffice = officeUnderTest

  def dummyId = aggregateId

  implicit def create: Gen[CreateDummy] = for {
    dummyId <- implicitly[Gen[EntityId]]
    name <- Gen.alphaStr
    description <- Gen.alphaStr
    value <- Gen.numStr
  } yield {
    CreateDummy(dummyId, name, description, value)
  }

  implicit def changeName: Gen[ChangeDummyName] = for {
    dummyId <- implicitly[Gen[EntityId]]
    name <- Gen.alphaStr
  } yield {
    ChangeDummyName(dummyId, name)
  }

  "Dummy office" should {
    "create Dummy" in {
      whenCommand {
        arbitrary[CreateDummy]
      }
      .expectEvent2 { c =>
        DummyCreated(dummyId, c.name, c.description, c.value)
      }
    }
  }

  "Dummy office" should {
    "update Dummy's name" in {
      givenCommand(
        arbitrary[CreateDummy]
      )
      .whenCommand { acks =>
        val oldName = acks.get[DummyCreated].name
        arbitrary[ChangeDummyName] suchThat (_.name != oldName)
      }
      .expectEvent2 { c =>
        DummyNameChanged(dummyId, c.name)
      }
    }
  }

  "Dummy office" should {
    "handle subsequent Update command" in {
      givenCommands(
        arbitrary[CreateDummy],
        arbitrary[ChangeDummyName]
      )
      .whenCommand {
        arbitrary[ChangeDummyName]
      }
      .expectEvent2 { c =>
        DummyNameChanged(dummyId, c.name)
      }
    }
  }

  "Dummy office" should {
    "reject null value" in {
      whenCommand {
        arbitrary[CreateDummy].sample.get.copy(value = null)
      }
      .expectException[RuntimeException]("null value not allowed")
    }
  }

}

