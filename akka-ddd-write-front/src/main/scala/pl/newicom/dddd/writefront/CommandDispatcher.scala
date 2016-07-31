package pl.newicom.dddd.writefront

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.office.RemoteOfficeId
import pl.newicom.dddd.writefront.CommandDispatcher.UnknownCommandClassException

import scala.concurrent.Future
import scala.util.Try

object CommandDispatcher {
  case class UnknownCommandClassException(command: Command) extends RuntimeException(s"No office registered for command: ${command.getClass.getName}")
}

trait CommandDispatcher extends GlobalOfficeClientSupport {
  this: Actor =>

  type OfficeResponse = Try[Any]

  implicit def timeout: Timeout

  import context.dispatcher

  def offices: Set[RemoteOfficeId[_]]

  def dispatch(msg: CommandMessage): Future[OfficeResponse] =
    officeRepresentative(msg).map {
      forward(msg)
    }.getOrElse {
      Future.failed(UnknownCommandClassException(msg.command))
    }


  private def officeRepresentative(msg: CommandMessage): Option[ActorRef] =
    offices.find(
      _.messageClass.isAssignableFrom(msg.command.getClass)
    ).map(
      officeActor
    )

  private def forward(msg: CommandMessage)(officeRepresentative: ActorRef): Future[OfficeResponse] =
    (officeRepresentative ? msg)
      .mapTo[Processed]
      .map(_.result)

}

