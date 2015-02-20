package pl.newicom.dddd.writefront

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.command.CommandMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait CommandHandler extends GlobalOfficeClientSupport {
  this: Actor =>

  def handle(officeName: String, command: CommandMessage)(implicit t: Timeout, ec: ExecutionContext): Future[Try[String]] = {
    office(officeName).ask(command).flatMap {
      case Processed(Success(_)) => Future(Success("Command processed. Thank you!"))
      case Processed(Failure(ex)) => Future(Failure(ex))
    }
  }
}