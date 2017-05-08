package pl.newicom.dddd.aggregate

import pl.newicom.dddd.messaging.event.OfficeEventMessage

trait ReplyConfig {
  type SuccessMapper = (Seq[OfficeEventMessage]) => Any
  def successMapper: SuccessMapper
}

trait SparseReply extends ReplyConfig {
  override def successMapper: SuccessMapper = (_) => "Command processed successfully. Thank you!"
}

trait ReplyWithEvents extends ReplyConfig {
  override def successMapper: SuccessMapper = r => r
}


