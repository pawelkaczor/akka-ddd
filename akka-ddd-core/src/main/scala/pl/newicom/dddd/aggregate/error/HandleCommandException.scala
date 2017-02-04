package pl.newicom.dddd.aggregate.error

import akka.actor.ActorRef
import pl.newicom.dddd.delivery.protocol.Receipt
import pl.newicom.dddd.messaging.command.CommandMessage

import scala.util.Failure

class HandleCommandException(val commandMessage: CommandMessage, val sender: ActorRef, val reason: Exception) extends RuntimeException(reason.getMessage) {

  def deliveryReceipt: Receipt = commandMessage.deliveryReceipt(Failure(reason))

}