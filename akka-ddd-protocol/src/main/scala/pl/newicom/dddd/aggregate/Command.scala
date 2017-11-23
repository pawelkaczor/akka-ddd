package pl.newicom.dddd.aggregate

trait Command {
  def aggregateId: AggregateId
}
