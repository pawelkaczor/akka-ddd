package pl.newicom.dddd.test.dummy

import akka.actor.{ActorPath, Props}
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.process.{Saga, SagaActorFactory, SagaConfig}
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.ValueChanged
import pl.newicom.dddd.test.dummy.DummySaga.DummyCommand

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
      case _ => throw new scala.RuntimeException("unknown event")
    }
  }

  case class DummyCommand(processId: EntityId, value: Int) extends Command {
    override def aggregateId: String = processId
  }
  
}

/**
 * <code>DummySaga</code> keeps a <code>counter</code> that is bumped whenever
 * <code>DummyEvent</code> is received containing <code>value</code> equal to <code>counter + 1</code>
 */
class DummySaga(override val pc: PassivationConfig, dummyOffice: Option[ActorPath]) extends Saga {

  override def persistenceId: String = s"DummySaga-$id"

  var counter: Int = 0

  def applyEvent = {
    case e =>
      val de = e.asInstanceOf[ValueChanged]
      counter = de.value.asInstanceOf[Int]
      context.system.eventStream.publish(e)
      if (dummyOffice.isDefined) {
        deliverCommand(dummyOffice.get, DummyCommand(de.id, counter))
      }
  }

  // see alternative implementation below
  def receiveEvent: Receive = {
    case em @ EventMessage(_, ValueChanged(_, value: Int, _)) if counter + 1 == value =>
      raise(em)
      log.debug(s"Processed event: $em")
  }

  // alternative implementation
  /*
      def receiveEvent: Receive = {
        case em: EventMessage => em.event match {
          case ValueChanged(_, value: Int) if currentValue + 1 == value =>
            raise(em)
            log.debug(s"Processed event: $em")
          case _ => handleUnexpectedEvent(em)
        }
      }
    */
}
