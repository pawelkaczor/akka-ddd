package pl.newicom.dddd.aggregate.error

class CommandRejected(msg: String) extends RuntimeException(msg)