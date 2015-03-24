package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.json4s.{Formats, FullTypeHints}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpecLike}
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.LocalOffice._
import pl.newicom.dddd.process.SagaManagerIntegrationSpec._
import pl.newicom.dddd.process.SagaSupport.{SagaManagerFactory, registerSaga}
import pl.newicom.dddd.test.dummy.DummySaga
import pl.newicom.dddd.test.dummy.DummySaga.{DummyEvent, DummySagaConfig}
import pl.newicom.dddd.utils.UUIDSupport.uuid7
import pl.newicom.eventstore.EventstoreSubscriber

import scala.concurrent.Await
import scala.concurrent.duration._

object SagaManagerIntegrationSpec {

  implicit val sys: ActorSystem = ActorSystem("SagaManagerIntegrationSpec")

  implicit def toEventMessage(te: DummyEvent): EventMessage = new EventMessage(te)

}

class SagaManagerIntegrationSpec extends TestKit(sys) with WordSpecLike with ImplicitSender
  with SagaManagerTestSupport with BeforeAndAfterAll with BeforeAndAfter  {

  override implicit val formats: Formats = defaultFormats + FullTypeHints(List(classOf[DummyEvent]))

  val dummyBpsName =  s"dummy-$uuid7"
  val processId = uuid7

  implicit val testSagaConfig = new DummySagaConfig(dummyBpsName)

  after {
    sys.terminate()
    Await.result(sys.whenTerminated, 5.seconds)
  }

  "SagaManager" should {
    "read events from bps" in {
      // Given
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[DummyEvent])
      storeEvents(dummyBpsName, DummyEvent(processId, 1), DummyEvent(processId, 2))

      // When
      var (sagaOffice, sagaManager) = registerSaga[DummySaga]

      // Then
      {
        probe.expectMsgAllClassOf(classOf[DummyEvent], classOf[DummyEvent])

        sagaManager ! "snap"
        sagaManager ! "numberOfUnconfirmed"
        expectMsg(0) // no unconfirmed events
        Thread.sleep(500)
      }

      ensureActorTerminated(sagaManager)

      storeEvents(dummyBpsName, DummyEvent(processId, 3), DummyEvent(processId, 5))
      sagaManager = registerSaga[DummySaga](sagaOffice)

      {
        probe.expectMsgClass(classOf[DummyEvent])

        sagaManager ! "snap"
        sagaManager ! "numberOfUnconfirmed"
        expectMsg(1) // single unconfirmed event: TestEvent(_, 5)
        Thread.sleep(500)

      }

      ensureActorTerminated(sagaManager)
      storeEvents(dummyBpsName, DummyEvent(processId, 4))
      sagaManager = registerSaga[DummySaga](sagaOffice)

      {
        probe.expectMsgAllClassOf(classOf[DummyEvent], classOf[DummyEvent])

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

  implicit def topLevelParent(implicit system: ActorSystem): CreationSupport = {
    new CreationSupport {
      override def getChild(name: String): Option[ActorRef] = None
      override def createChild(props: Props, name: String): ActorRef = {
        system.actorOf(props, name)
      }
    }
  }

  def ensureActorTerminated(actor: ActorRef) = {
    watch(actor)
    actor ! PoisonPill
    fishForMessage(1.seconds) {
      case Terminated(_) =>
        unwatch(actor)
        true
      case _ => false
    }
  }

}
