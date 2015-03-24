package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.TestProbe
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.office.LocalOffice._
import pl.newicom.dddd.process.SagaManagerIntegrationSpec._
import pl.newicom.dddd.process.SagaSupport.{SagaManagerFactory, registerSaga}
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{ChangeValue, CreateDummy, ValueChanged}
import pl.newicom.dddd.test.dummy.{DummyAggregateRoot, DummySaga}
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaConfig}
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.eventstore.EventstoreSubscriber

import scala.concurrent.duration._

object SagaManagerIntegrationSpec {

  implicit val sys: ActorSystem = ActorSystem("SagaManagerIntegrationSpec")

  implicit def actorFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it
    }

}

class SagaManagerIntegrationSpec extends OfficeSpec[DummyAggregateRoot] {

  def dummyId = aggregateId
  implicit lazy val testSagaConfig = new DummySagaConfig(dummyId)


  "SagaManager" should {
    "read events from bps" in {
      // Given
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[ValueChanged])
      ignoreMsg({ case Processed(_) => true })

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

      // When
      var (sagaOffice, sagaManager) = registerSaga[DummySaga]

      // Then
      {
        probe.expectMsgAllClassOf(classOf[ValueChanged], classOf[ValueChanged])

        sagaManager ! "snap"
        sagaManager ! "numberOfUnconfirmed"
        expectMsg(0) // no unconfirmed events
        Thread.sleep(500)
      }

      ensureActorUnderTestTerminated(sagaManager)


      when {
        ChangeValue(dummyId, 3)
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 3L)
      }

      when {
        ChangeValue(dummyId, 5)
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 4L)
      }

      sagaManager = registerSaga[DummySaga](sagaOffice)

      {
        probe.expectMsgClass(classOf[ValueChanged])

        sagaManager ! "snap"
        sagaManager ! "numberOfUnconfirmed"
        expectMsg(1) // single unconfirmed event: TestEvent(_, 5)
        Thread.sleep(500)

      }

      ensureActorUnderTestTerminated(sagaManager)

      when {
        ChangeValue(dummyId, 4)
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 5L)
      }

      sagaManager = registerSaga[DummySaga](sagaOffice)

      {
        probe.expectMsgAllClassOf(classOf[ValueChanged], classOf[ValueChanged])

        sagaManager ! "snap"
        sagaManager ! "numberOfUnconfirmed"
        expectMsg(0) // no unconfirmed events
        Thread.sleep(500)
      }

    }
  }

  implicit val sagaManagerFactory: SagaManagerFactory = (sagaConfig, sagaOffice) => {
    new SagaManager(sagaConfig, sagaOffice) with EventstoreSubscriber {
      override def redeliverInterval = 1.seconds
      override def receiveCommand: Receive = myReceive.orElse(super.receiveCommand)
      def myReceive: Receive = {
        case "numberOfUnconfirmed" => sender() ! numberOfUnconfirmed
      }
    }
  }

}
