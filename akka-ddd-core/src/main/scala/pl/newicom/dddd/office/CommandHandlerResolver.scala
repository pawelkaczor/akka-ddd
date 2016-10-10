package pl.newicom.dddd.office

import pl.newicom.dddd.aggregate.Command

class CommandHandlerResolver(officeIDs: List[RemoteOfficeId[_]]) extends Function[Command, RemoteOfficeId[_]] {

  override def apply(command: Command): RemoteOfficeId[_] = {
    val handlerOpt = officeIDs.find(_.handles(command))
    if (handlerOpt.isDefined)
      handlerOpt.get
    else
      throw new RuntimeException(s"No Office registered capable of handling ${command.getClass.getName}")
  }
}
