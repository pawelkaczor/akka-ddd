package pl.newicom.dddd.messaging

import akka.actor.{ActorRef, Stash}
import scala.concurrent.duration._

object CollaborationSupport {
  case object ReceiveTimeout

  @SerialVersionUID(1L)
  class CollaborationFailed(msg: String) extends RuntimeException(msg)

  case class NoResponseReceived(timeout: FiniteDuration)
    extends CollaborationFailed(s"No response received within $timeout.")

  case class UnexpectedResponseReceived(response: Any)
    extends CollaborationFailed(s"Unexpected response received: $response.")

}

trait CollaborationSupport extends Stash {
  import CollaborationSupport._

  implicit class CollaborationBuilder(val target: ActorRef) {
    def !<(msg: Any): Collaboration = Collaboration(target, msg)
  }

  case class Collaboration(target: ActorRef, msg: Any) {
    def apply(receive: Receive)(implicit timeout: FiniteDuration): Unit = {
      target ! msg
      internalExpectOnce(target, receive)
    }

    def expectOnce(receive: Receive)(implicit timeout: FiniteDuration): Unit = apply(receive)
  }

  private def internalExpectOnce(target: ActorRef, receive: Receive)(implicit timeout: FiniteDuration): Unit = {
    import context.dispatcher
    val scheduledTimeout = context.system.scheduler.scheduleOnce(timeout, self, ReceiveTimeout)

    context.become(
      receive andThen {
        case _ => // expected response received
          scheduledTimeout.cancel()
          unstashAll()
          context.unbecome()
      } orElse {
        case ReceiveTimeout =>
          throw NoResponseReceived(timeout)

        case msg if sender() eq target =>
          throw UnexpectedResponseReceived(msg)

        case _  =>
          stash()
      }
      , discardOld = false)
  }

}
