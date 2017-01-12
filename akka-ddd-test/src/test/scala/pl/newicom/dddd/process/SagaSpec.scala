package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.alod.Delivered
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{CaseId, EventMessage, OfficeEventMessage}
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.office.OfficeListener
import pl.newicom.dddd.test.dummy.DummyProtocol.ValueChanged
import pl.newicom.dddd.test.dummy.DummySaga
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaConfig, EventApplied}
import pl.newicom.dddd.test.support.TestConfig
import pl.newicom.dddd.utils.UUIDSupport.uuid10

import scala.concurrent.duration._


class SagaSpec extends TestKit(TestConfig.testSystem) with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  implicit lazy val testSagaConfig = new DummySagaConfig("DummySaga")

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val _ = new OfficeListener[DummySaga]

  implicit object TestSagaActorFactory extends SagaActorFactory[DummySaga] {
    override def props(pc: PassivationConfig): Props =
      Props(new DummySaga(pc, officeId, None))
  }

  def processId: EntityId = uuid10
  lazy val coordinationOffice: ActorRef = office[DummySaga].actor

  "Saga" should {
    "not process previously processed events" in {
      // Given
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[EventApplied])

      val em1 = toEventMessage(ValueChanged(processId, 1, 1L))

      // When
      coordinationOffice ! em1
      coordinationOffice ! em1

      // Then
      probe.expectMsgClass(classOf[EventApplied])
      probe.expectNoMsg(1.seconds)
    }
  }

  "Saga" should {
    "process messages received in order" in {
      // Given
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[EventApplied])

      val em1 = toEventMessage(ValueChanged(processId, 1, 1L))

      // When
      coordinationOffice ! em1
      // Then
      probe.expectMsgClass(classOf[EventApplied])

      // When
      coordinationOffice ! toEventMessage(ValueChanged(processId, 2, 2L), previouslySentMsg = Some(em1))
      // Then
      probe.expectMsgClass(classOf[EventApplied])
      probe.expectNoMsg(1.seconds)
    }
  }

  "Saga" should {
    "not process message received out of order" in {
      // Given
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[EventApplied])

      val em1 = toEventMessage(ValueChanged(processId, 1, 1L))
      val em2 = toEventMessage(ValueChanged(processId, 2, 2L), previouslySentMsg = Some(em1))
        .withMustFollow(Some("0"))

      // When
      coordinationOffice ! em1
      // Then
      probe.expectMsgClass(classOf[EventApplied])

      // When
      coordinationOffice ! em2
      // Then
      probe.expectNoMsg(1.seconds)
    }
  }

  "Saga" should {
    "acknowledge previously processed events" in {
      // Given
      val em1 = toEventMessage(ValueChanged(processId, 1, 1L))

      // When/Then
      coordinationOffice ! em1
      expectMsgClass(classOf[Delivered])

      coordinationOffice ! em1
      expectMsgClass(classOf[Delivered])
    }
  }

  def toEventMessage(event: ValueChanged, previouslySentMsg: Option[EventMessage] = None): EventMessage = {
    val entityId = previouslySentMsg.flatMap(msg => msg.correlationId).getOrElse(processId)
    OfficeEventMessage(CaseId(entityId, event.dummyVersion), event).withMetaData(Map(
      CorrelationId -> entityId,
      DeliveryId -> 1L
    )).withMustFollow(previouslySentMsg.map(msg => msg.id))
  }

  def ensureActorTerminated(actor: ActorRef): Any = {
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
