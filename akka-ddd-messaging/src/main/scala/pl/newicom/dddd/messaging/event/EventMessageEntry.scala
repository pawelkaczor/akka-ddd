package pl.newicom.dddd.messaging.event

import org.joda.time.DateTime

case class EventMessageEntry(msg: OfficeEventMessage, position: Long, created: Option[DateTime])
