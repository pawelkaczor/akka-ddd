package pl.newicom.dddd

import pl.newicom.dddd.aggregate.EntityId

trait BusinessEntity {
  def id: EntityId
  def department: String
}


