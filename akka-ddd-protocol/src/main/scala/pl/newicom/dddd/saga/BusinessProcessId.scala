package pl.newicom.dddd.saga

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.{BusinessEntity, Eventsourced}

case class BusinessProcessId(processDomain: String, processId: EntityId, department: String = null) extends BusinessEntity with Eventsourced {
  def id: EntityId = processId

  val domain: Eventsourced = BusinessProcessDomain(processDomain, department)
}
