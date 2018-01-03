package pl.newicom.dddd.actor

import pl.newicom.dddd.messaging.event.EventMessage

trait RespondingPolicy {
  type SuccessMapper = (Seq[EventMessage]) => Any
  def successMapper: SuccessMapper
}

case object SparseReply extends RespondingPolicy {
  override def successMapper: SuccessMapper = (_) => "Command processed successfully. Thank you!"
}

case object ReplyWithEvents extends RespondingPolicy {
  override def successMapper: SuccessMapper = r => r
}


