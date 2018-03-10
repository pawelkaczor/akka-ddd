package pl.newicom.dddd.test.dummy

import java.util.UUID

import pl.newicom.dddd.aggregate
import pl.newicom.dddd.aggregate.AggregateId

object DummyProtocol {
  //
  // Commands
  //

  type DummyId = AggregateId

  sealed trait Command extends aggregate.Command {
    def id: DummyId
    override def aggregateId: DummyId = id
  }
  sealed trait UpdateCommand extends Command

  case class CreateDummy(id: DummyId, name: String, description: String, value: Value) extends Command

  case class ChangeName(id: DummyId, name: String)                                   extends UpdateCommand
  case class ChangeDescription(id: DummyId, description: String)                     extends UpdateCommand
  case class ChangeValue(id: DummyId, value: Int)                                    extends UpdateCommand
  case class GenerateValue(id: DummyId)                                              extends UpdateCommand
  case class ConfirmGeneratedValue(id: DummyId, confirmationToken: UUID)             extends UpdateCommand
  case class Reset(id: DummyId, name: String)                                        extends UpdateCommand

  //
  // Events
  //
  sealed trait DummyEvent {
    def id: DummyId
  }
  case class DummyCreated(id: DummyId, name: String, description: String, value: Int) extends DummyEvent
  case class NameChanged(id: DummyId, name: String)                                   extends DummyEvent
  case class DescriptionChanged(id: DummyId, description: String)                     extends DummyEvent
  case class ValueChanged(id: DummyId, value: Int, dummyVersion: Long)                extends DummyEvent
  case class ValueGenerated(id: DummyId, value: Int, confirmationToken: UUID)         extends DummyEvent
  case class ValueGeneratedSuccessfully(id: DummyId)         extends DummyEvent

  case class CandidateValue(value: Int, confirmationToken: UUID)

  case class Value(value: Int) extends AnyVal
}
