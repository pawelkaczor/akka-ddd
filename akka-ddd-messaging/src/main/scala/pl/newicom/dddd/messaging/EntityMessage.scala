package pl.newicom.dddd.messaging

import pl.newicom.dddd.aggregate.EntityId

trait EntityMessage {
  def entityId: EntityId
  def payload: Any
}
