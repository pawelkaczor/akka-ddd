package pl.newicom.dddd.aggregate

class AggregateId(val value: String) extends AnyVal {
  override def toString: String = value
}
