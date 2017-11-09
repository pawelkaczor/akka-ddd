package pl.newicom.dddd.saga

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.{BusinessEntity, Eventsourced}

case class BusinessProcessDomain(id: EntityId, department: String) extends BusinessEntity with Eventsourced
