package pl.newicom.dddd.aggregate

import pl.newicom.dddd.messaging.event.OfficeEventMessage

trait RespondingPolicy {
  type SuccessMapper = (Seq[OfficeEventMessage]) => Any
  def successMapper: SuccessMapper
}

case object SparseReply extends RespondingPolicy {
  override def successMapper: SuccessMapper = (_) => "Command processed successfully. Thank you!"
}

case object ReplyWithEvents extends RespondingPolicy {
  override def successMapper: SuccessMapper = r => r
}


