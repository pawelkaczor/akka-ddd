package pl.newicom.dddd.messaging

import pl.newicom.dddd.aggregate.EntityId

trait AddressableMessage {
  def destination: Option[EntityId]
  def payload: Any

  def payloadName: String = payload.getClass.getSimpleName
}
