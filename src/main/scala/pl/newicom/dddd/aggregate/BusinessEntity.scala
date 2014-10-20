package pl.newicom.dddd.aggregate

import pl.newicom.dddd.aggregate.BusinessEntity.EntityId

object BusinessEntity {
  type EntityId = String
}

trait BusinessEntity {
  def id: EntityId
}


