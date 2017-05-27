package pl.newicom.dddd.aggregate

abstract class Query {
  def aggregateId: String
  type R
}
