package pl.newicom.dddd.process

import pl.newicom.dddd.aggregate.{AggregateId, Command, EntityId}

case class EnqueueCommand(command: Command, officeId: EntityId, department: String) extends Command {

  def aggregateId: AggregateId =
    command.aggregateId
}
