package pl.newicom.dddd.test.dummy

import akka.actor.{ActorPath, Props}
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.process.{Saga, SagaActorFactory, SagaConfig}
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{DummyCreated, ValueChanged}
import pl.newicom.dddd.test.dummy.DummySaga.{EventApplied, DummyCommand}

object DummySaga {

  implicit def defaultSagaIdResolution[A]: EntityIdResolution[A] = new EntityIdResolution[A]

  implicit object DummySagaActorFactory extends SagaActorFactory[DummySaga] {
    override def props(pc: PassivationConfig): Props = {
      Props(new DummySaga(pc, None))
    }
  }

  class DummySagaConfig(bpsName: String) extends SagaConfig[DummySaga](bpsName) {
    def correlationIdResolver = {
      case ValueChanged(pId, _, _) => pId
      case DummyCreated(pId, _, _, _) => pId
      case other => throw new scala.RuntimeException(s"unknown event: ${other.getClass.getName}")
    }
  }

  case class DummyCommand(processId: EntityId, value: Int) extends Command {
    override def aggregateId: String = processId
  }

  case class EventApplied(e: DomainEvent)
}

/**
 * <code>DummySaga</code> keeps a <code>counter</code> that is bumped whenever
 * <code>DummyEvent</code> is received containing <code>value</code> equal to <code>counter + 1</code>
 * <code>DummySaga</code> publishes all applied events to local actor system bus.
 */
class DummySaga(override val pc: PassivationConfig, dummyOffice: Option[ActorPath]) extends Saga {

  override def persistenceId: String = s"DummySaga-$id"

  var counter: Int = 0

  def applyEvent = {
    case e @ ValueChanged(id, value, _) =>
      counter = value
      context.system.eventStream.publish(EventApplied(e))
      log.debug(s"Applied event message: ${eventMessage}")
      if (dummyOffice.isDefined) {
        deliverCommand(dummyOffice.get, DummyCommand(id, counter))
      }
  }

  // see alternative implementation below
  def receiveEvent: Receive = {
    case em @ EventMessage(_, ValueChanged(_, value: Int, _)) if counter + 1 == value =>
      raise(em)
      log.debug(s"Processed event: $em")
    case em @ EventMessage(_, DummyCreated(_, _, _, _)) =>
      raise(em)
    case other =>
      log.debug(other.toString)
  }

  // alternative implementation
  /*
      def receiveEvent: Receive = {
        case em: EventMessage => em.event match {
          case ValueChanged(_, value: Int) if currentValue + 1 == value =>
            raise(em)
            log.debug(s"Processed event: $em")
          case dc: DummyCreated => // ignore
          case _ => handleUnexpectedEvent(em)
        }
      }
    */
}
