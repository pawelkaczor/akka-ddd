package pl.newicom.dddd.eventhandling.reliable

import pl.newicom.dddd.messaging.event.OfficeEventMessage

case class RedeliveryFailedException(event: OfficeEventMessage) extends RuntimeException
