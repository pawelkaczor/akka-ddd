package pl.newicom.dddd.test.dummy

import akka.actor.Props
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.office.{LocalOfficeId, Office, RemoteOfficeId}
import pl.newicom.dddd.process._
import pl.newicom.dddd.saga.ProcessConfig
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{DummyCreated, ValueChanged}
import pl.newicom.dddd.test.dummy.DummySaga.{DummyCommand, DummyState, EventApplied}

object DummySaga {

  class DummySagaConfig(bpsName: String) extends ProcessConfig[DummySaga](bpsName) {

    override val id = bpsName

    def correlationIdResolver = {
      case ValueChanged(pId, _, _) => pId
      case DummyCreated(pId, _, _, _) => pId
      case other => throw new scala.RuntimeException(s"unknown event: ${other.getClass.getName}")
    }
  }

  implicit object DummySagaActorFactory extends SagaActorFactory[DummySaga] {
    override def props(pc: PassivationConfig): Props = {
      Props(new DummySaga(pc, officeId, None))
    }
  }

  case class DummyCommand(processId: EntityId, value: Int) extends Command {
    override def aggregateId: String = processId
  }

  case class EventApplied(e: DomainEvent)

  case class DummyState(counter: Int) extends SagaState[DummyState]

}

/**
 * <code>DummySaga</code> keeps a <code>counter</code> that is bumped whenever
 * <code>DummyEvent</code> is received containing <code>value</code> equal to <code>counter + 1</code>
 * <code>DummySaga</code> publishes all applied events to local actor system bus.
 */
class DummySaga(val pc: PassivationConfig,
                val officeId: LocalOfficeId[DummySaga],
                dummyOffice: Option[Office]) extends ProcessManager[DummyState] {

  startWhen {

    case e: DummyCreated => DummyState(e.value)
    case e: ValueChanged => DummyState(0)

  } andThen {

    case DummyState(counter) => {

      case e @ ValueChanged(id, value, _) if state.counter + 1 == value =>

        DummyState(value) {

            context.system.eventStream.publish(EventApplied(e))
            if (dummyOffice.isDefined) {
              dummyOffice.get !! DummyCommand(id, counter)
            }

        }

    }

  }

  override def processCollaborators: List[RemoteOfficeId[_]] = List()
}
