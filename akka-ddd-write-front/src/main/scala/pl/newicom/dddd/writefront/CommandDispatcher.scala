package pl.newicom.dddd.writefront

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.Formats
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.office.RemoteOfficeId
import pl.newicom.dddd.utils.UUIDSupport._
import pl.newicom.dddd.http.JsonMarshalling
import pl.newicom.dddd.streams.ImplicitMaterializer

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait CommandDispatcher extends GlobalOfficeClientSupport with CommandDirectives
  with Directives with ImplicitMaterializer with JsonMarshalling {
  this: Actor =>

  import context.dispatcher

  def offices: Set[RemoteOfficeId[_]]

  def dispatch[A <: Command](implicit f: Formats, t: Timeout): Route = commandTimestamp { timestamp =>
    commandManifest[A] { implicit cManifest =>
      post {
        entity(as[A]) { command =>
          complete {
            val target = officeActor(command)
            if (target.isDefined) {
              val cm = CommandMessage(command, uuid, timestamp.toDate)

              delegate(cm, target.get).map[ToResponseMarshallable] {
                case Success(msg) => StatusCodes.OK -> msg
                case Failure(ex)  => StatusCodes.InternalServerError -> ex.getMessage
              }

            } else {
                StatusCodes.UnprocessableEntity -> s"No office registered for command: ${command.getClass.getName}"
            }
          }
        }
      }
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