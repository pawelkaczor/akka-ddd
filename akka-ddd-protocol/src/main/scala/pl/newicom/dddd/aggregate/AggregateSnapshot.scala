package pl.newicom.dddd.aggregate

case class AggregateSnapshot(state: Any, receivedMsgIds: Set[String])