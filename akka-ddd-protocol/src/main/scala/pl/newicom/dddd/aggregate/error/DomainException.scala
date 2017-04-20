package pl.newicom.dddd.aggregate.error

class DomainException(msg: String) extends CommandRejected(msg)