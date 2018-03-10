package pl.newicom.dddd.aggregate

case class AggregateId(value: String) extends AnyVal {
  override def toString: String = value
}
