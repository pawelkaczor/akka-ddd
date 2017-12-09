package pl.newicom.dddd.test.dummy

import akka.actor.Props
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.office.{LocalOfficeId, OfficeRef}
import pl.newicom.dddd.process._
import pl.newicom.dddd.saga.{BusinessProcessId, ProcessConfig}
import pl.newicom.dddd.test.dummy.DummyProtocol.{DummyCreated, DummyId, ValueChanged}
import pl.newicom.dddd.test.dummy.DummySaga.{DummyCommand, DummyState, EventApplied, Poison}
import pl.newicom.dddd.utils.UUIDSupport.uuid7

object DummySaga {

  val Poison: Int = 100

  class DummySagaConfig(bpsName: String) extends ProcessConfig[DummySaga](BusinessProcessId(bpsName, uuid7)) {

    override val id: EntityId = bpsName

    def correlationIdResolver: PartialFunction[DomainEvent, EntityId] = {
      case ValueChanged(pId, _, _) => pId.value
      case DummyCreated(pId, _, _, _) => pId.value
      case other => throw new scala.RuntimeException(s"unknown event: ${other.getClass.getName}")
    }
  }

  implicit object DummySagaActorFactory extends SagaActorFactory[DummySaga] {
    override def props(pc: PassivationConfig): Props = {
      Props(new DummySaga(pc, officeId, None))
    }
  }

  case class DummyCommand(processId: DummyId, value: Int) extends Command {
    override def aggregateId: DummyId = processId
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
                dummyOffice: Option[OfficeRef]) extends ProcessManager[DummyState] {

  startWhen {

    case e: DummyCreated => DummyState(e.value)
    case _: ValueChanged => DummyState(0)

  } andThen {

    case DummyState(counter) => {

      case ValueChanged(_, Poison, _) =>
        sys.error("Poison value detected!")

      case e @ ValueChanged(id, value, _) if counter + 1 == value =>

        DummyState(value) {

            context.system.eventStream.publish(EventApplied(e))
            if (dummyOffice.isDefined) {
              dummyOffice.get !! DummyCommand(id, counter)
            }

        }

    }

  }

}
