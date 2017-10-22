package pl.newicom.dddd.office
import akka.actor.ActorRef
import akka.util.Timeout
import pl.newicom.dddd.aggregate.error.CommandHandlerNotDefined
import pl.newicom.dddd.aggregate.{Command, Query}
import pl.newicom.dddd.delivery.protocol.{DeliveryHandler, Processed}
import pl.newicom.dddd.office.OfficeStub.CommandHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Failure

object OfficeStub {
  type CommandHandler = PartialFunction[Command, Processed]

  def apply(officeId: OfficeId, commandHandler: CommandHandler): OfficeHandler =
    new OfficeStub(officeId, commandHandler)
}

class OfficeStub(val officeId: OfficeId, commandHandler: CommandHandler) extends OfficeHandler {

  def !(msg: Any)(implicit sender: ActorRef): Unit = ???

  def !!(msg: Any)(implicit dh: DeliveryHandler): Unit = ???

  def ?(command: Command)(implicit ex: ExecutionContext, t: Timeout, sender: ActorRef): Future[Processed] =
    Future.successful(
      commandHandler.applyOrElse(
        command,
        (c: Command) => Processed(Failure(CommandHandlerNotDefined(c.getClass.getSimpleName)))))

  def ?(query: Query)(implicit ex: ExecutionContext, t: Timeout, ct: ClassTag[query.R], sender: ActorRef): Future[query.R] = ???
}
