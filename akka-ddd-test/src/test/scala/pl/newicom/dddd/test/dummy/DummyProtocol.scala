package pl.newicom.dddd.test.dummy

import java.util.UUID

import pl.newicom.dddd.aggregate
import pl.newicom.dddd.aggregate.EntityId

object DummyProtocol {
  //
  // Commands
  //
  sealed trait Command extends aggregate.Command {
    def id: EntityId
    override def aggregateId: String = id
  }
  sealed trait UpdateCommand extends Command

  case class CreateDummy(id: EntityId, name: String, description: String, value: Int) extends Command

  case class ChangeName(id: EntityId, name: String)                                   extends UpdateCommand
  case class ChangeDescription(id: EntityId, description: String)                     extends UpdateCommand
  case class ChangeValue(id: EntityId, value: Int)                                    extends UpdateCommand
  case class GenerateValue(id: EntityId)                                              extends UpdateCommand
  case class ConfirmGeneratedValue(id: EntityId, confirmationToken: UUID)             extends UpdateCommand
  case class Reset(id: EntityId, name: String)                                        extends UpdateCommand

  //
  // Events
  //
  sealed trait DummyEvent {
    def id: EntityId
  }
  case class DummyCreated(id: EntityId, name: String, description: String, value: Int) extends DummyEvent
  case class NameChanged(id: EntityId, name: String)                                   extends DummyEvent
  case class DescriptionChanged(id: EntityId, description: String)                     extends DummyEvent
  case class ValueChanged(id: EntityId, value: Int, dummyVersion: Long)                extends DummyEvent
  case class ValueGenerated(id: EntityId, value: Int, confirmationToken: UUID)         extends DummyEvent

  case class CandidateValue(value: Int, confirmationToken: UUID)

}
