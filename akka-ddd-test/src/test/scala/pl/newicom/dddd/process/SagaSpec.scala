package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import pl.newicom.dddd.actor.{DefaultConfig, PassivationConfig}
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.alod.Delivered
import pl.newicom.dddd.messaging.MetaAttribute.{Correlation_Id, Delivery_Id}
import pl.newicom.dddd.messaging.{MetaAttribute, MetaData}
import pl.newicom.dddd.messaging.event.{EventMessage, OfficeEventMessage}
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.office.{CaseRef, OfficeListener}
import pl.newicom.dddd.test.dummy.DummyProtocol.{DummyId, ValueChanged}
import pl.newicom.dddd.test.dummy.DummySaga
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaConfig, EventApplied}
import pl.newicom.dddd.test.support.TestConfig
import pl.newicom.dddd.utils.UUIDSupport.uuid10

import scala.concurrent.duration._


class SagaSpec extends TestKit(TestConfig.testSystem) with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  implicit lazy val testSagaConfig: DummySagaConfig = new DummySagaConfig("DummySaga")

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val _ = new OfficeListener[DummySaga]

  implicit object TestSagaActorFactory extends SagaActorFactory[DummySaga] {
    override def props(pc: PassivationConfig): Props =
      Props(new DummySaga(DefaultConfig(pc, replyWithEvents = false), None))
  }

  def processId: EntityId = uuid10
  lazy val coordinationOffice: ActorRef = office[DummySaga].actor

  "Saga" should {
    "not process previously processed events" in {
      // Given
      val probe = testProbe
      val em1 = eventMessage(ValueChanged(new DummyId(processId), 1, 1L))

      // When
      coordinationOffice ! em1
      coordinationOffice ! em1

      // Then
      probe.expectMsgClass(classOf[EventApplied])
      probe.expectNoMessage(1.seconds)
    }
  }

  "Saga" should {
    "process messages received in order" in {
      // Given
      val probe = testProbe
      val em1 = eventMessage(ValueChanged(new DummyId(processId), 1, 1L))

      // When
      coordinationOffice ! em1
      // Then
      probe.expectMsgClass(classOf[EventApplied])

      // When
      coordinationOffice ! eventMessage(ValueChanged(new DummyId(processId), 2, 2L), previouslySentMsg = Some(em1))
      // Then
      probe.expectMsgClass(classOf[EventApplied])
      probe.expectNoMessage(1.seconds)
    }
  }

  "Saga" should {
    "not process message received out of order" in {
      // Given
      val probe = testProbe
      val em1 = eventMessage(ValueChanged(new DummyId(processId), 1, 1L))
      val em2 = eventMessage(ValueChanged(new DummyId(processId), 2, 2L), previouslySentMsg = Some(em1))
        .withMustFollow(Some("0"))

      // When
      coordinationOffice ! em1
      // Then
      probe.expectMsgClass(classOf[EventApplied])

      // When
      coordinationOffice ! em2
      // Then
      probe.expectNoMessage(1.seconds)
    }
  }

  "Saga" should {
    "acknowledge previously processed events" in {
      // Given
      val em1 = eventMessage(ValueChanged(new DummyId(processId), 1, 1L))

      // When/Then
      coordinationOffice ! em1
      expectMsgClass(classOf[Delivered])

      coordinationOffice ! em1
      expectMsgClass(classOf[Delivered])
    }
  }

  def eventMessage(event: ValueChanged, previouslySentMsg: Option[EventMessage] = None): EventMessage = {
    val entityId = previouslySentMsg.flatMap(msg => msg.correlationId).getOrElse(processId)
    OfficeEventMessage(CaseRef(entityId, testSagaConfig, Some(event.dummyVersion)), event, MetaData(
      MetaAttribute.Id -> uuid10,
      Correlation_Id -> entityId,
      Delivery_Id -> 1L
    )).withMustFollow(previouslySentMsg.map(_.id))
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

  def testProbe: TestProbe = {
    val probe = TestProbe()
    system.eventStream.subscribe(probe.ref, classOf[EventApplied])
    probe
  }

}
