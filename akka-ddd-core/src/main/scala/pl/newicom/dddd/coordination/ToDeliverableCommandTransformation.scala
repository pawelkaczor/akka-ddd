package pl.newicom.dddd.coordination

import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.office.OfficeRegistryImpl
import pl.newicom.dddd.process.EnqueueCommand

class ToDeliverableCommandTransformation(officeRegistry: OfficeRegistryImpl) extends Function[Command, Command] {

  def apply(command: Command): Command = {
    val officeId = officeRegistry.commandHandlerId(command)
    if (officeRegistry.isOfficeAvailableInCluster(officeId.id)) {
      command
    } else {
      EnqueueCommand(command, officeId.id, officeId.department)
    }
  }

}
