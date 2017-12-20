package pl.newicom.dddd.test.dummy

import org.scalacheck.ScalacheckShapeless._
import akka.actor.Props
import org.scalacheck.{Arbitrary, Gen}
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.test.dummy.DummyProtocol._
import pl.newicom.dddd.test.support.TestConfig._
import DummyWithGenSpec._
import pl.newicom.dddd.aggregate.error.DomainException
import pl.newicom.dddd.office.OfficeRef
import pl.newicom.dddd.test.ar.ARSpec
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.DummyConfig

object DummyWithGenSpec {

  implicit def actorFactory: AggregateRootActorFactory[DummyAggregateRoot] =
    AggregateRootActorFactory[DummyAggregateRoot](pc => Props(new DummyAggregateRoot(DummyConfig(pc))))
}

class DummyWithGenSpec extends ARSpec[DummyEvent, DummyAggregateRoot](Some(testSystem)) {

  def dummyOffice: OfficeRef = officeUnderTest

  def dummyId: DummyId = aggregateId

  //
  // Custom generators
  //
  implicit def aValue: A[Value] = Arbitrary { Gen.choose(1, 1000).map(Value) }

  "Dummy office" should {
    /**
     * Commands are generated automatically (custom generators can be provided, see generators above).
     * The command under test (created inside When clause)
     * is accessible from inside Then/expect clause.
     *
     * No need to define val members for name, description, ...
     * No need to define concrete values for name, description, ...
     */
    "create Dummy" in {
      when {
        a [CreateDummy]
      }
      .expect { c =>
        DummyCreated(c.id, c.name, c.description, c.value.value)
      }
    }

    /**
     * Events triggered by Given command(s) are accessible from inside When clause.
     * Properties of generated command can be changed inside test body.
     */
    "update Dummy's name" in {
      given {
        a [CreateDummy]
      }
      .when { implicit hist =>
        a[ChangeName].copy(name = past[DummyCreated].name.toLowerCase)
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    /**
      * Multiple commands in When section are supported.
      * 'expect' in Then section is able to handle multiple expected events.
      */
    "update Dummy's value twice" in {
      given {
        a [CreateDummy]
      }
      .when {
        ChangeValue(dummyId, 1) & ChangeValue(dummyId, 2)
      }
      .expect { c =>
        ValueChanged(dummyId, 1, 1) & ValueChanged(dummyId, 2, 2)
      }
    }

    /**
     * No problem with two or more commands inside Given clause.
     */
    "handle subsequent Update command" in {
      given(
        a_list_of [CreateDummy, ChangeName]
      )
      .when {
        a [ChangeName]
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    /**
     * Events triggered by Given command(s) are accessible from inside Then/expect clause.
     */
    "confirm generated value" in {
      given(
        a_list_of [CreateDummy, GenerateValue]
      )
      .when { implicit hist =>
        ConfirmGeneratedValue(dummyId, past[ValueGenerated].confirmationToken)
      }
      .expect { implicit c =>
        ValueChanged(c.id, past[ValueGenerated].value, dummyVersion = 1)
      }
    }

    /**
     * Another example showing how to modify generated command.
     */
    "reject negative value" in {
      when {
        a[CreateDummy].copy(value = Value(-1))
      }
      .expectException[DomainException]("negative value not allowed")
    }
  }


}

