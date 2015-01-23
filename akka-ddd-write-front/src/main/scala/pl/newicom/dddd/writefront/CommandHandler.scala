package pl.newicom.dddd.writefront

import akka.actor.Actor
import akka.http.model.{StatusCode, StatusCodes}
import akka.pattern.ask
import akka.util.Timeout
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.Acknowledged

import scala.concurrent.{ExecutionContext, Future}

trait CommandHandler extends GlobalOfficeClientSupport {
  this: Actor =>

  def handle(officeName: String, command: Command)(implicit timeout: Timeout, ec: ExecutionContext): Future[StatusCode] = {
    office(officeName).ask(command).flatMap {
      case Acknowledged => Future(StatusCodes.OK)
      case _ => Future(StatusCodes.InternalServerError)
    }
  }
}