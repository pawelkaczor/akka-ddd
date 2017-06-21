package pl.newicom.dddd.aggregate

import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.office.CaseRef

case class CommandHandlerContext[C <: Config](caseRef: CaseRef, config: C, commandMetaData: MetaData)
