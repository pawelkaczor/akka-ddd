package pl.newicom.dddd.writefront

import java.util.Date

import akka.actor.Actor
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import org.json4s.Formats
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.http.JsonMarshalling
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.streams.ImplicitMaterializer
import pl.newicom.dddd.utils.UUIDSupport.uuid
import pl.newicom.dddd.writefront.CommandDispatcher.UnknownCommandClassException

import scala.util.{Failure, Success, Try}

trait HttpCommandHandler extends CommandDispatcher with CommandDirectives with Directives with JsonMarshalling with ImplicitMaterializer {
  this: Actor =>

  type OfficeResponseToClientResponse = (Try[Any]) => ToResponseMarshallable

  import context.dispatcher

  def handle[A <: Command](implicit f: Formats): Route =
    commandManifest[A] { implicit cManifest =>
      post {
        entity(as[A]) { command =>
          complete {
            dispatch(toCommandMessage(command)) map toClientResponse
          }
        }
      }
  }

  def toCommandMessage(command: Command): CommandMessage =
    CommandMessage(command, uuid, new Date)

  def toClientResponse: OfficeResponseToClientResponse =  {
    case Success(result) =>
      StatusCodes.OK -> result.toString

    case Failure(UnknownCommandClassException(command))=>
      StatusCodes.UnprocessableEntity -> s"No office registered for command: ${command.getClass.getName}"

    case Failure(ex) =>
      StatusCodes.InternalServerError -> ex.getMessage
  }

}
