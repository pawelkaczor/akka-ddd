package pl.newicom.dddd.aggregate.error

class QueryRejected(msg: String) extends RuntimeException(msg)