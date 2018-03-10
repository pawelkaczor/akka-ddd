package pl.newicom.dddd.aggregate

abstract class Query {
  def aggregateId: AggregateId
  type R
}
