package pl.newicom.dddd.writefront

import akka.actor.Actor
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.util.Timeout
import org.json4s.Formats
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.http.JsonMarshalling
import pl.newicom.dddd.streams.ImplicitMaterializer

import scala.util.{Failure, Success, Try}

trait HttpCommandHandler extends CommandDispatcher with CommandDirectives with Directives with JsonMarshalling with ImplicitMaterializer {
  this: Actor =>

  type OfficeResponseToClientResponse = (Try[String]) => ToResponseMarshallable

  import context.dispatcher

  def handle[A <: Command](implicit f: Formats, t: Timeout): Route = commandManifest[A] { implicit cManifest =>
    post {
      entity(as[A]) { command =>
        complete {
          dispatch(command) map toClientResponse
        }
      }
    }
  }

  def toClientResponse: OfficeResponseToClientResponse =  {
    case Success(msg) =>
      StatusCodes.OK -> msg

    case Failure(UnknownCommandClassException(command))=>
      StatusCodes.UnprocessableEntity -> s"No office registered for command: ${command.getClass.getName}"

    case Failure(ex) =>
      StatusCodes.InternalServerError -> ex.getMessage
  }

}
