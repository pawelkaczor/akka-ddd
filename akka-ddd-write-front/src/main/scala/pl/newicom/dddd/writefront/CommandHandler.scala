package pl.newicom.dddd.writefront

import akka.actor.{ActorRef, ExtendedActorSystem, Extension, ExtensionKey}
import akka.http.model.{StatusCode, StatusCodes}
import akka.pattern.ask
import akka.util.Timeout
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.Acknowledged

import scala.concurrent.{ExecutionContext, Future}

object CommandHandler extends ExtensionKey[CommandHandler]

class CommandHandler(system: ExtendedActorSystem) extends Extension {

  def handle(office: ActorRef, command: Command)(implicit timeout: Timeout, ec: ExecutionContext): Future[StatusCode] = {
    office.ask(command).flatMap {
      case Acknowledged => Future(StatusCodes.OK)
      case _ => Future(StatusCodes.InternalServerError)
    }
  }
}