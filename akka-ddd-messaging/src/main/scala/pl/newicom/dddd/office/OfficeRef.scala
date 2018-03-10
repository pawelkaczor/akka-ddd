package pl.newicom.dddd.office

import akka.actor.{ActorPath, ActorRef}
import akka.util.Timeout
import pl.newicom.dddd.aggregate.{Command, EntityId, Query}
import pl.newicom.dddd.delivery.protocol.{DeliveryHandler, Processed}
import pl.newicom.dddd.utils.ImplicitUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object LocalOfficeId {

  implicit def fromRemoteId[E : ClassTag](remoteId: RemoteOfficeId[_]): LocalOfficeId[E] =
    LocalOfficeId[E](remoteId.id, remoteId.department, Some(remoteId.commandClass))
}

case class LocalOfficeId[E : ClassTag](id: EntityId, department: String, messageClass: Option[Class[_]] = None) extends OfficeId {

  def caseClass: Class[E] =
    implicitly[ClassTag[E]].runtimeClass.asParameterizedBy[E]

  def caseName: String = caseClass.getSimpleName
}

trait OfficeRefLike {
  def officeId: OfficeId
  def id: EntityId = officeId.id
  def department: String = officeId.department

  def ?(command: Command)(implicit sender: ActorRef, ex: ExecutionContext, t: Timeout): Future[Processed]
  def ?(query: Query)(implicit ex: ExecutionContext, t: Timeout, ct: ClassTag[query.R], sender: ActorRef): Future[query.R]
}

trait CommandHandler {
  def !!(msg: Any)(implicit dh: DeliveryHandler): Unit
}

class OfficeRef(override val officeId: OfficeId, val actor: ActorRef) extends OfficeRefLike with CommandHandler {
  import akka.pattern.ask

  def actorPath: ActorPath = actor.path

  def !!(msg: Any)(implicit dh: DeliveryHandler): Unit =
    dh((actorPath, msg))

  def ?(command: Command)(implicit sender: ActorRef, ex: ExecutionContext, t: Timeout): Future[Processed] =
    (actor ? command).mapTo[Processed]

  def ?(query: Query)(implicit ex: ExecutionContext, t: Timeout, ct: ClassTag[query.R], sender: ActorRef): Future[query.R] =
    (actor ? query).mapTo[query.R]

}