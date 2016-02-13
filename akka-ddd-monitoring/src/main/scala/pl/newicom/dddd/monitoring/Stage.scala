package pl.newicom.dddd.monitoring

import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.AddressableMessage

case class Stage(position: Integer, shortName: String) {

  def traceContextName(observed: BusinessEntity, msg: AddressableMessage): String =
    s"$position-${observed.department.capitalize}-$shortName-${msg.payloadName}"
}

object Stage {
  val Reception_Of_Command = Stage(1, "reception")
  val Handling_Of_Command  = Stage(2, "handling")
  val Reception_Of_Event   = Stage(3, "reception")
  val Reaction_On_Event    = Stage(4 ,"reaction")
}
