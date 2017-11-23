package pl.newicom.dddd.messaging.query

import pl.newicom.dddd.aggregate.{EntityId, Query}
import pl.newicom.dddd.messaging.AddressableMessage

case class QueryMessage(query: Query) extends AddressableMessage {

  def destination: Option[EntityId] = Some(query.aggregateId.value)

  def payload: Any = query
}