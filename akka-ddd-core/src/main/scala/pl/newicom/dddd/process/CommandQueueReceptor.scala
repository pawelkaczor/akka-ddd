package pl.newicom.dddd.process

import pl.newicom.dddd.coordination.{ReceptorBuilder, ReceptorConfig}
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.OfficeRegistryImpl

object CommandQueueReceptor {

  def apply(department: String)(officeRegistry: OfficeRegistryImpl): ReceptorConfig =
    ReceptorBuilder(s"command-queue-$department")
      .reactTo(commandQueue(department))
      .applyTransduction {
        case EventMessage(_, CommandEnqueued(command, officeId, _)) =>
          val officeRef = officeRegistry.officeRef(officeId)
          CommandMessage(command).withTarget(officeRef.actorPath.toSerializationFormat)
      }
      .autoRoute
      .copy(isSupporting_MustFollow_Attribute = false)

}
