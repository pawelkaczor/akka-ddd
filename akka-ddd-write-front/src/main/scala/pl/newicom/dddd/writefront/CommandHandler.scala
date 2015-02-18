package pl.newicom.dddd.writefront

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.Processed

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait CommandHandler extends GlobalOfficeClientSupport {
  this: Actor =>

  def handle(officeName: String, command: Command)(implicit timeout: Timeout, ec: ExecutionContext): Future[Try[String]] = {
    office(officeName).ask(command).flatMap {
      case Processed(Success(_)) => Future(Success("Command processed. Thank you!"))
      case Processed(Failure(ex)) => Future(Failure(ex))
    }
  }
}