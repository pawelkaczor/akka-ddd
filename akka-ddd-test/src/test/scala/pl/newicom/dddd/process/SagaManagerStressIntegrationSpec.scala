package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.TestProbe
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.office.OfficeListener
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.process.SagaManagerIntegrationSpec._
import pl.newicom.dddd.process.SagaSupport.SagaManagerFactory
import pl.newicom.dddd.persistence.{RegularSnapshottingConfig, SaveSnapshotRequest}
import pl.newicom.dddd.saga.SagaOffice
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{ChangeValue, CreateDummy, ValueChanged}
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaActorFactory, DummySagaConfig, EventApplied}
import pl.newicom.dddd.test.dummy.{DummyAggregateRoot, DummySaga, dummyOfficeId}
import pl.newicom.dddd.test.support.IntegrationTestConfig.integrationTestSystem
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.eventstore.EventstoreSubscriber

import scala.concurrent.duration._

object SagaManagerStressIntegrationSpec {

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
class SagaManagerStressIntegrationSpec extends OfficeSpec[DummyAggregateRoot](Some(integrationTestSystem("SagaManagerStressSpec"))) {

  def dummyId = aggregateId

  implicit lazy val testSagaConfig = new DummySagaConfig(s"${dummyOfficeId.id}-$dummyId")

  implicit val _ = new OfficeListener[DummySaga]

  implicit val sagaManagerFactory: SagaManagerFactory[DummySaga] = (sagaOffice: SagaOffice[DummySaga]) => {
    new SagaManager[DummySaga]()(sagaOffice) with EventstoreSubscriber {

      override def receiveCommand: Receive = myReceive.orElse(super.receiveCommand)

      def myReceive: Receive = {
        case GetNumberOfUnconfirmed => sender() ! numberOfUnconfirmed
      }

      override lazy val config: ReceptorConfig = defaultConfig.copy(capacity = 1000)
      override val snapshottingConfig = RegularSnapshottingConfig(receiveEvent, interval = 50)
    }
  }


  val sagaProbe = TestProbe()
  system.eventStream.subscribe(sagaProbe.ref, classOf[EventApplied])
  ignoreMsg({ case EventMessage(_, Processed(_)) => true })

  "SagaManager" should {

    var sagaManager: ActorRef = null
    var sagaOffice: SagaOffice[DummySaga] = null

    val changes = 2 to 101

    "deliver 100 events to a saga office" in {
      val so = office[DummySaga].asInstanceOf[SagaOffice[DummySaga]]
      val sm = SagaSupport.sagaManager(so)
      sagaManager = sm; sagaOffice = so

      given {
        List(
          CreateDummy(dummyId, "name", "description", 0),
          ChangeValue(dummyId, 1)
        )
      }
      .when {
        changes.map(v => ChangeValue(dummyId, v))
      }
      .expectEvents(
        changes.map(v => ValueChanged(dummyId, v, v.toLong)): _*
      )

      expectNumberOfEventsAppliedBySaga(changes.size + 1)
      expectNoUnconfirmedMessages(sagaManager)
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

  def expectNumberOfUnconfirmedMessages(sagaManager: ActorRef, expectedNumberOfMessages: Int): Unit = within(3.seconds) {
    sagaManager ! SaveSnapshotRequest
    awaitAssert {
      sagaManager ! GetNumberOfUnconfirmed
      expectMsg(expectedNumberOfMessages)
    }
  }
}
