package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.TestProbe
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.coordination.ReceptorConfig
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.OfficeFactory.coordinationOffice
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.process.SagaIntegrationSpec._
import pl.newicom.dddd.persistence.SaveSnapshotRequest
import pl.newicom.dddd.saga.CoordinationOffice
import pl.newicom.dddd.test.ar.ARSpec
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.DummyConfig
import pl.newicom.dddd.test.dummy.DummyProtocol._
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaActorFactory, DummySagaConfig, EventApplied, Poison}
import pl.newicom.dddd.test.dummy.{DummyAggregateRoot, DummySaga, dummyOfficeId}
import pl.newicom.dddd.test.support.IntegrationTestConfig.integrationTestSystem
import pl.newicom.eventstore.EventstoreSubscriber

import scala.concurrent.duration._

object SagaIntegrationSpec {

  case object GetNumberOfUnconfirmed

  implicit def actorFactory: AggregateRootActorFactory[DummyAggregateRoot] =
    AggregateRootActorFactory[DummyAggregateRoot](pc => Props(new DummyAggregateRoot(DummyConfig(pc))))
}

/**
 * Requires EventStore to be running on localhost!
 */
class SagaIntegrationSpec extends ARSpec[DummyEvent, DummyAggregateRoot](Some(integrationTestSystem("ReceptorIntegrationSpec"))) {

  override val shareAggregateRoot = true

  def dummyId: DummyId = aggregateId

  implicit lazy val testSagaConfig: DummySagaConfig = new DummySagaConfig(s"${dummyOfficeId.id}-$dummyId")

  implicit lazy val receptorActorFactory: ReceptorActorFactory[DummySaga] = new ReceptorActorFactory[DummySaga] {
    override def receptorFactory: ReceptorFactory = (config: ReceptorConfig) => {
      new Receptor(config) with EventstoreSubscriber {
        override def redeliverInterval: FiniteDuration = 1.seconds
        override def receiveCommand: Receive = myReceive.orElse(super.receiveCommand)
        def myReceive: Receive = {
          case GetNumberOfUnconfirmed => sender() ! numberOfUnconfirmed
        }

      }
    }
  }

  var receptor: ActorRef = _

  implicit lazy val officeListener: CoordinationOfficeListener[DummySaga] = new CoordinationOfficeListener[DummySaga] {
    override def officeStarted(office: CoordinationOffice[DummySaga], receptorRef: ActorRef): Unit = {
      receptor = receptorRef
    }
  }


  val sagaProbe = TestProbe()
  system.eventStream.subscribe(sagaProbe.ref, classOf[EventApplied])
  ignoreMsg({ case EventMessage(_, Processed(_)) => true })

  "Saga" should {

    var coordOffice: CoordinationOffice[DummySaga] = null

    "confirm accepted event" in {
      // given
      given {
        CreateDummy(dummyId, "name", "description", Value(0))
      }
      .when {
        ChangeValue(dummyId, 1)
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 1L)
      }

      // when
      val co = coordinationOffice[DummySaga]
      coordOffice = co

      // then
      expectNumberOfEventsAppliedBySaga(1)
      expectNoUnconfirmedMessages(receptor)
    }

    "confirm dropped event" in {
      // given
      ensureTerminated(receptor) // stop events delivery
      when {
        ChangeValue(dummyId, 3) // bump counter by 2, DummySaga should drop this event
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 2L)
      }

      // when
      receptor = receptorActorFactory(coordOffice.receptorConfig)

      // then
      expectNoUnconfirmedMessages(receptor)
    }

    "not confirm event if event processing failed" in {
      // given
      ensureTerminated(receptor)
      when {
        ChangeValue(dummyId, Poison) // Saga should fail
      }
      .expect { c =>
        ValueChanged(dummyId, c.value, 3L)
      }

      // when
      receptor = receptorActorFactory(coordOffice.receptorConfig)

      // then
      expectNumberOfUnconfirmedMessages(receptor, 1)

    }

    "receive unconfirmed event after receptor restarted" in {
      // given
      ensureTerminated(receptor)

      // when
      receptor = receptorActorFactory(coordOffice.receptorConfig)

      // then
      expectNumberOfUnconfirmedMessages(receptor, 1)

    }

  }

  def expectNumberOfEventsAppliedBySaga(expectedNumberOfEvents: Int): Unit = {
    for (_ <- 1 to expectedNumberOfEvents) {
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
