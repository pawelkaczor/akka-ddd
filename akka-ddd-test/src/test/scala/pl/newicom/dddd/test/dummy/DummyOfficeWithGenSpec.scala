package pl.newicom.dddd.test.dummy

import akka.actor.Props
import org.scalacheck.Gen
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._
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

  implicit def changeName: Gen[ChangeName] = for {
    dummyId <- implicitly[Gen[EntityId]]
    name <- Gen.alphaStr
  } yield {
    ChangeName(dummyId, name)
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
      .whenCommand { implicit ctx =>
        arbitrary[ChangeName] suchThat (_.name != past[DummyCreated].name)
      }
      .expectEvent2 { c =>
        NameChanged(dummyId, c.name)
      }
    }
  }

  "Dummy office" should {
    "handle subsequent Update command" in {
      givenCommands(
        arbitrary[CreateDummy],
        arbitrary[ChangeName]
      )
      .whenCommand {
        arbitrary[ChangeName]
      }
      .expectEvent2 { c =>
        NameChanged(dummyId, c.name)
      }
    }
  }

  "Dummy office" should {
    "confirm generated value" in {
      givenCommands(
        arbitrary[CreateDummy],
        GenerateValue(dummyId)
      )
      .whenCommand { implicit ctx =>
        ConfirmGeneratedValue(dummyId, past[ValueGenerated].confirmationToken)
      }
      .expectEvent2 { implicit ctx =>
        ValueChanged(dummyId, past[ValueGenerated].value, dummyVersion = 2)
      }
    }
  }

  "Dummy office" should {
    "reject null value" in {
      whenCommand {
        arbitrary[CreateDummy] map (_ copy(value = null))
        // alternatively:
        //arbitraryOf[CreateDummy](_ copy(value = null))
      }
      .expectException[RuntimeException]("null value not allowed")
    }
  }

}

