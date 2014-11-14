package pl.newicom.dddd.delivery

import akka.actor.{ActorRef, ActorSystem}
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.{Receipt, Confirm}
import pl.newicom.dddd.messaging.Message
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.serialization.SerializationSupport

import scala.language.implicitConversions
import scala.reflect.ClassTag

object DeliveryContext {

  case object ReceiptsRequested

  type ReceiptRequested = (Class[_], Array[Byte])

  case object DeliveryId

  object Adjust {

    implicit def toCommandMessage(command: Command)(implicit system: ActorSystem) = new Confirmable(CommandMessage(command))

    implicit def toConfirmable(message: Message)(implicit system: ActorSystem) = {
      new Confirmable(message)
    }

    case class Confirmable(msg: Message)(implicit _system: ActorSystem) extends ReadonlyConfirmable(msg) {

      /**
       * request ddd.support.domain.protocol.Confirm
       */
      def requestConfirmation(deliveryId: Long)(implicit requester: ActorRef) = {
        val msg: Message = requestDLR[Confirm]
        msg.withMetaAttribute[Message](DeliveryId, deliveryId)
      }

      /**
       * dlr - delivery receipt
       */
      def requestDLR[A](implicit t: ClassTag[A], requester: ActorRef) = {
        msg.withMetaAttribute[Message](ReceiptsRequested,
          msg.tryGetMetaAttribute[Set[ReceiptRequested]](ReceiptsRequested).getOrElse(Set[ReceiptRequested]())
            .+((t.runtimeClass, serialize(requester))))
      }
    }

  }

  implicit def toReadonlyConfirmable(message: Message)(implicit system: ActorSystem) = {
    new ReadonlyConfirmable(message)
  }

  class ReadonlyConfirmable(srcMsg: Message)(implicit _system: ActorSystem) extends SerializationSupport {

    override protected def system = _system

    def anyReceiptRequested: Boolean = {
      srcMsg.hasMetaAttribute(ReceiptsRequested)
    }

    def receiptRequester(receipt: Receipt): Option[ActorRef] = {
      srcMsg.tryGetMetaAttribute[Set[ReceiptRequested]](ReceiptsRequested)
        .flatMap(_.find(rr => rr._1.equals(receipt.getClass)).map(rr => deserialize[ActorRef](rr._2)))
    }

    def sendReceiptIfRequested(receipt: Receipt): Unit = {
      receiptRequester(receipt).foreach(_ ! receipt)
    }

    /**
     * Send ddd.support.domain.protocol.Confirm to requester
     */
    def confirmIfRequested(): Unit = {
      if (srcMsg.hasMetaAttribute(DeliveryId)) {
        sendReceiptIfRequested(Confirm(srcMsg.getMetaAttribute[Long](DeliveryId)))
      }
    }

  }

}
