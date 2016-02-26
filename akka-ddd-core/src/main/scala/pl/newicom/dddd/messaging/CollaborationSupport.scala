package pl.newicom.dddd.messaging

import akka.actor.{Actor, ActorRef, Stash}
import pl.newicom.dddd.messaging.CollaborationSupport.{NoResponseReceived, ReceiveTimeout, UnexpectedResponseReceived}

import scala.concurrent.duration.{FiniteDuration, _}

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
  this: Actor =>

  def expectFrom(collaborator: ActorRef)(receive: Receive)(implicit timeout: FiniteDuration = 3.seconds): Unit = {
    import context.dispatcher
    val scheduledTimeout = scheduler.scheduleOnce(timeout, self, ReceiveTimeout)

    context.become(
      receive andThen {
        case _ => // expected response received
          scheduledTimeout.cancel()
          unstashAll()
          context.unbecome()
      } orElse {
        case ReceiveTimeout =>
          throw NoResponseReceived(timeout)

        case msg if sender() eq collaborator =>
          throw UnexpectedResponseReceived(msg)

        case _  =>
          stash()
      }
      , discardOld = false)
  }

  private def scheduler = context.system.scheduler
}
