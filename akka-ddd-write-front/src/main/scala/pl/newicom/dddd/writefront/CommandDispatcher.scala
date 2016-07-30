package pl.newicom.dddd.writefront

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.office.RemoteOfficeId
import pl.newicom.dddd.utils.UUIDSupport._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait CommandDispatcher extends GlobalOfficeClientSupport {
  this: Actor =>

  import context.dispatcher

  def offices: Set[RemoteOfficeId[_]]

  def dispatch[A <: Command](command: A)(implicit t: Timeout) = {
    val target = officeActor(command)
    if (target.isDefined) {
      delegate(CommandMessage(command, uuid, new java.util.Date), target.get)
    } else {
      Future.failed(UnknownCommandClassException(command))
    }
  }

  private def officeActor(c: Command): Option[ActorRef] =
    offices.find(
      _.messageClass.isAssignableFrom(c.getClass)
    ).map(
      officeActor
    )

  private def delegate(cm: CommandMessage, target: ActorRef)(implicit t: Timeout): Future[Try[String]] =
    target.ask(cm).flatMap {
      case Processed(Success(_)) => Future(Success("Command processed. Thank you!"))
      case Processed(Failure(ex)) => Future(Failure(ex))
    }

}

case class UnknownCommandClassException(command: Command) extends RuntimeException(s"No office registered for command: ${command.getClass.getName}")