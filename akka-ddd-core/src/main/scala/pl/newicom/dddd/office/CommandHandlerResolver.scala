package pl.newicom.dddd.office

import akka.actor.ActorSystem
import pl.newicom.dddd.aggregate.Command

class CommandHandlerResolver()(implicit as: ActorSystem) extends Function[Command, OfficeId] {

  val officeRegistry = OfficeRegistry(as)

  override def apply(command: Command): OfficeId = {
    val handlerOpt = officeRegistry.find(_.handles(command))
    if (handlerOpt.isDefined)
      handlerOpt.get
    else
      throw new RuntimeException(s"No Office registered capable of handling ${command.getClass.getName}")
  }
}
