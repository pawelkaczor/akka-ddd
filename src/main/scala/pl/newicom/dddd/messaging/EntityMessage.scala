package pl.newicom.dddd.messaging

import pl.newicom.dddd.aggregate.BusinessEntity
import BusinessEntity.EntityId
import pl.newicom.dddd.aggregate.BusinessEntity

trait EntityMessage {
  def entityId: EntityId
  def payload: Any
}
