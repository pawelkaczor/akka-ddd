package pl.newicom.dddd.saga

import pl.newicom.dddd.aggregate._

case class BusinessProcess(id: EntityId, department: String) extends BusinessEntity
