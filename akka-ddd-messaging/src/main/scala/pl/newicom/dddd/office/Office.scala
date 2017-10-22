package pl.newicom.dddd.office

import akka.actor.{ActorPath, ActorRef}
import akka.util.Timeout
import pl.newicom.dddd.aggregate.{Command, EntityId, Query}
import pl.newicom.dddd.delivery.protocol.{DeliveryHandler, Processed}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object LocalOfficeId {

  implicit def fromRemoteId[E : ClassTag](remoteId: RemoteOfficeId[_]): LocalOfficeId[E] =
    LocalOfficeId[E](remoteId.id, remoteId.department)
}

case class LocalOfficeId[E : ClassTag](id: EntityId, department: String) extends OfficeId {

  def caseClass: Class[E] =
    implicitly[ClassTag[E]].runtimeClass.asInstanceOf[Class[E]]

  def caseName: String = caseClass.getSimpleName
}

trait OfficeHandler {
  def officeId: OfficeId
  def id: EntityId = officeId.id
  def department: String = officeId.department

  def !(msg: Any)(implicit sender: ActorRef): Unit
  def !!(msg: Any)(implicit dh: DeliveryHandler): Unit

  def ?(command: Command)(implicit ex: ExecutionContext, t: Timeout, sender: ActorRef): Future[Processed]
  def ?(query: Query)(implicit ex: ExecutionContext, t: Timeout, ct: ClassTag[query.R], sender: ActorRef): Future[query.R]
}

class Office(override val officeId: OfficeId, val actor: ActorRef) extends OfficeHandler {
  import akka.pattern.ask

  def actorPath: ActorPath = actor.path

  def !(msg: Any)(implicit sender: ActorRef): Unit =
    actor ! msg

  def !!(msg: Any)(implicit dh: DeliveryHandler): Unit =
    deliver(msg)

  def ?(command: Command)(implicit ex: ExecutionContext, t: Timeout, sender: ActorRef): Future[Processed] =
    (actor ? command).mapTo[Processed]

  def ?(query: Query)(implicit ex: ExecutionContext, t: Timeout, ct: ClassTag[query.R], sender: ActorRef): Future[query.R] =
    (actor ? query).mapTo[query.R]

  private def deliver(msg: Any)(implicit dh: DeliveryHandler): Unit =
    dh((actorPath, msg))

}