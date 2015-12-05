package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.WordSpecLike
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.delivery.protocol.alod.Delivered
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.ValueChanged
import pl.newicom.dddd.test.dummy.DummySaga
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaConfig, EventApplied}
import pl.newicom.dddd.test.support.TestConfig
import pl.newicom.dddd.utils.UUIDSupport.uuid10

import scala.concurrent.duration._


class SagaSpec extends TestKit(TestConfig.testSystem) with WordSpecLike with ImplicitSender {

  implicit lazy val testSagaConfig = new DummySagaConfig("DummySaga")

  implicit object TestSagaActorFactory extends SagaActorFactory[DummySaga] {
    override def props(pc: PassivationConfig): Props = {
      Props(new DummySaga(pc, officeId, None) {
        override def onEventReceived(em: EventMessage, action: SagaAction) = {
          action match {
            case RejectEvent =>
              system.eventStream.publish(em.event)
            case _ =>
          }
          super.onEventReceived(em, action)
        }
      })
    }
  }

  def processId = uuid10
  val sagaOffice = office[DummySaga].actor

  "Saga" should {
    "not process previously processed events" in {
      // Given
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[EventApplied])

      val em1 = toEventMessage(ValueChanged(processId, 1, 1L))

      // When
      sagaOffice ! em1
      sagaOffice ! em1

      // Then
      probe.expectMsgClass(classOf[EventApplied])
      probe.expectNoMsg(1.seconds)
    }
  }

  "Saga" should {
    "acknowledge previously processed events" in {
      // Given
      val em1 = toEventMessage(ValueChanged(processId, 1, 1L))

      // When/Then
      sagaOffice ! em1
      expectMsgClass(classOf[Delivered])

      sagaOffice ! em1
      expectMsgClass(classOf[Delivered])
    }
  }

  def toEventMessage(e: ValueChanged): EventMessage = {
    EventMessage(e).withMetaData(Map(
      CorrelationId -> processId,
      DeliveryId -> 1L
    ))
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
