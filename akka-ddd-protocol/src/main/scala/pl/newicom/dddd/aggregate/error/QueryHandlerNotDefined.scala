package pl.newicom.dddd.aggregate.error

import QueryHandlerNotDefined._

object QueryHandlerNotDefined {
  def msg(queryName: String) = s"$queryName can not be processed: missing query handler!"
}

case class QueryHandlerNotDefined(queryName: String) extends QueryRejected(msg(queryName))
