package pl.newicom.dddd.process

import pl.newicom.dddd.aggregate.{Command, EntityId}

case class CommandEnqueued(command: Command, officeId: EntityId, department: String)