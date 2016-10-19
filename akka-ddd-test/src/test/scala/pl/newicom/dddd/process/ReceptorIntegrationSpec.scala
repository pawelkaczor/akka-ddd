package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.TestProbe
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.coordination.ReceptorConfig
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.OfficeFactory.office
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.process.ReceptorIntegrationSpec._
import pl.newicom.dddd.persistence.SaveSnapshotRequest
import pl.newicom.dddd.process.ReceptorSupport.{ReceptorFactory, receptor}
import pl.newicom.dddd.saga.CoordinationOffice
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{ChangeValue, CreateDummy, ValueChanged}
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaActorFactory, DummySagaConfig, EventApplied}
import pl.newicom.dddd.test.dummy.{DummyAggregateRoot, DummySaga, dummyOfficeId}
import pl.newicom.dddd.test.support.IntegrationTestConfig.integrationTestSystem
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.eventstore.EventstoreSubscriber

import scala.concurrent.duration._

object ReceptorIntegrationSpec {

  case object GetNumberOfUnconfirmed

  implicit def actorFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it
    }

}

/**
 * Requires EventStore to be running on localhost!
 */
class ReceptorIntegrationSpec extends OfficeSpec[DummyAggregateRoot](Some(integrationTestSystem("ReceptorIntegrationSpec"))) {

  override val shareAggregateRoot = true

  def dummyId: EntityId = aggregateId

  implicit lazy val testSagaConfig = new DummySagaConfig(s"${dummyOfficeId.id}-$dummyId")

  implicit val receptorFactory: ReceptorFactory = (config: ReceptorConfig) => {
    new Receptor(config) with EventstoreSubscriber {
      override def redeliverInterval: FiniteDuration = 1.seconds
      override def receiveCommand: Receive = myReceive.orElse(super.receiveCommand)

      def myReceive: Receive = {
        case GetNumberOfUnconfirmed => sender() ! numberOfUnconfirmed
      }

    }
  }

  var rc: ActorRef = _

  implicit val _ = new CoordinationOfficeListener[DummySaga] {
    override def officeStarted(office: CoordinationOffice[DummySaga], receptor: ActorRef): Unit = {
      rc = receptor
    }
  }


  val sagaProbe = TestProbe()
  system.eventStream.subscribe(sagaProbe.ref, classOf[EventApplied])
  ignoreMsg({ case EventMessage(_, Processed(_)) => true })

  "Receptor" should {

    var coordinationOffice: CoordinationOffice[DummySaga] = null

    "deliver events to the receiver" in {
      // given
      given {
        List(
          CreateDummy(dummyId, "name", "description", 0),
          ChangeValue(dummyId, 1)
        )
      }
      .when {
        ChangeValue(dummyId, 2)
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 2L)
      }

      // when
      val co = office[DummySaga].asInstanceOf[CoordinationOffice[DummySaga]]
      coordinationOffice = co

      // then
      expectNumberOfEventsAppliedBySaga(2)
      expectNoUnconfirmedMessages(rc)
    }

    "persist unconfirmed events" in {
      // given
      ensureActorUnderTestTerminated(rc) // stop events delivery
      when {
        ChangeValue(dummyId, 3) // bump counter by 1, DummySaga should accept this event
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 3L)
      }

      when {
        ChangeValue(dummyId, 5) // bump counter by 2, DummySaga should not accept this event
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 4L)
      }

      // when
      rc = receptor(coordinationOffice.receptorConfig) // start events delivery, number of events to be delivered to DummySaga is 2

      // then
      expectNumberOfEventsAppliedBySaga(1)
      expectNumberOfUnconfirmedMessages(rc, 1) // single unconfirmed event: ValueChanged(_, 5)
    }

    "redeliver unconfirmed events to the receiver" in {
      // given
      ensureActorUnderTestTerminated(rc)
      when {
        ChangeValue(dummyId, 4)
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 5L)
      }

      // when
      rc = receptor(coordinationOffice.receptorConfig)

      // then
      expectNumberOfEventsAppliedBySaga(1)
      expectNumberOfUnconfirmedMessages(rc, 0)

    }
  }

  def expectNumberOfEventsAppliedBySaga(expectedNumberOfEvents: Int): Unit = {
    for (i <- 1 to expectedNumberOfEvents) {
      sagaProbe.expectMsgClass(classOf[EventApplied])
    }
  }

  def expectNoUnconfirmedMessages(receptor: ActorRef): Unit = {
    expectNumberOfUnconfirmedMessages(receptor, 0)
  }

  def expectNumberOfUnconfirmedMessages(receptor: ActorRef, expectedNumberOfMessages: Int): Unit = within(5.seconds) {
    receptor ! SaveSnapshotRequest
    awaitAssert {
      receptor ! GetNumberOfUnconfirmed
      expectMsg(expectedNumberOfMessages)
    }
  }
}
