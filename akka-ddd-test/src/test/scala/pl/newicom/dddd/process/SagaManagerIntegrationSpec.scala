package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.TestProbe
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.process.SagaManagerIntegrationSpec._
import pl.newicom.dddd.process.SagaSupport.{SagaManagerFactory, registerSaga}
import pl.newicom.dddd.persistence.SaveSnapshotRequest
import pl.newicom.dddd.saga.SagaOffice
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{ChangeValue, CreateDummy, ValueChanged}
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaActorFactory, DummySagaConfig, EventApplied}
import pl.newicom.dddd.test.dummy.{dummyOfficeId, DummyAggregateRoot, DummySaga}
import pl.newicom.dddd.test.support.IntegrationTestConfig.integrationTestSystem
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.eventstore.EventstoreSubscriber
import scala.concurrent.duration._

object SagaManagerIntegrationSpec {

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
class SagaManagerIntegrationSpec extends OfficeSpec[DummyAggregateRoot](Some(integrationTestSystem("SagaManagerIntegrationSpec"))) {

  override val shareAggregateRoot = true

  def dummyId = aggregateId

  implicit lazy val testSagaConfig = new DummySagaConfig(s"${dummyOfficeId.id}-$dummyId")

  implicit val sagaManagerFactory: SagaManagerFactory[DummySaga] = (sagaOffice: SagaOffice[DummySaga]) => {
    new SagaManager[DummySaga]()(sagaOffice) with EventstoreSubscriber {
      override def redeliverInterval = 1.seconds
      override def receiveCommand: Receive = myReceive.orElse(super.receiveCommand)

      def myReceive: Receive = {
        case GetNumberOfUnconfirmed => sender() ! numberOfUnconfirmed
      }

    }
  }


  val sagaProbe = TestProbe()
  system.eventStream.subscribe(sagaProbe.ref, classOf[EventApplied])
  ignoreMsg({ case EventMessage(_, Processed(_)) => true })

  "SagaManager" should {

    var sagaManager: ActorRef = null
    var sagaOffice: SagaOffice[DummySaga] = null

    "deliver events to a saga office" in {
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
      val (so, sm) = registerSaga[DummySaga]
      sagaManager = sm; sagaOffice = so

      // then
      expectNumberOfEventsAppliedBySaga(2)
      expectNoUnconfirmedMessages(sagaManager)
    }

    "persist unconfirmed events" in {
      // given
      ensureActorUnderTestTerminated(sagaManager) // stop events delivery
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
      sagaManager = registerSaga[DummySaga](sagaOffice) // start events delivery, number of events to be delivered to DummySaga is 2

      // then
      expectNumberOfEventsAppliedBySaga(1)
      expectNumberOfUnconfirmedMessages(sagaManager, 1) // single unconfirmed event: ValueChanged(_, 5)
    }

    "redeliver unconfirmed events to a saga office" in {
      // given
      ensureActorUnderTestTerminated(sagaManager)
      when {
        ChangeValue(dummyId, 4)
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 5L)
      }

      // when
      sagaManager = registerSaga[DummySaga](sagaOffice)

      // then
      expectNumberOfEventsAppliedBySaga(1)
      expectNumberOfUnconfirmedMessages(sagaManager, 0)

    }
  }

  def expectNumberOfEventsAppliedBySaga(expectedNumberOfEvents: Int): Unit = {
    for (i <- 1 to expectedNumberOfEvents) {
      sagaProbe.expectMsgClass(classOf[EventApplied])
    }
  }

  def expectNoUnconfirmedMessages(sagaManager: ActorRef): Unit = {
    expectNumberOfUnconfirmedMessages(sagaManager, 0)
  }

  def expectNumberOfUnconfirmedMessages(sagaManager: ActorRef, expectedNumberOfMessages: Int): Unit = within(5.seconds) {
    sagaManager ! SaveSnapshotRequest
    awaitAssert {
      sagaManager ! GetNumberOfUnconfirmed
      expectMsg(expectedNumberOfMessages)
    }
  }
}
