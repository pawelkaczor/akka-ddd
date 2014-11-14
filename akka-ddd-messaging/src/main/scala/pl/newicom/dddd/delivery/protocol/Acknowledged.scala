package pl.newicom.dddd.delivery.protocol

import pl.newicom.dddd.messaging.Message

case object Acknowledged

case class Acknowledged(msg: Message)
