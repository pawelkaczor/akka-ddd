package pl.newicom.dddd.office

import akka.actor.{ActorPath, ActorRef}
import pl.newicom.dddd.aggregate.{BusinessEntity, EntityId}
import pl.newicom.dddd.cluster.DefaultDistributionStrategy
import pl.newicom.dddd.delivery.protocol.DeliveryHandler

import scala.reflect.ClassTag

trait OfficeId extends BusinessEntity {
  def clerkGlobalId(clerkId: EntityId): String =
    s"$id-$clerkId"

  def clerk(clerkId: EntityId): BusinessEntity =
    Clerk(clerkGlobalId(clerkId), department)

  def distributionStrategy = new DefaultDistributionStrategy
}

case class Clerk(id: EntityId, department: String) extends BusinessEntity

case class RemoteOfficeId[M : ClassTag](id: EntityId, department: String, messageClass: Class[M]) extends OfficeId

object LocalOfficeId {

  implicit def fromRemoteId[E : ClassTag](remoteId: RemoteOfficeId[_]): LocalOfficeId[E] =
    LocalOfficeId[E](remoteId.id, remoteId.department)
}

case class LocalOfficeId[E : ClassTag](id: EntityId, department: String) extends OfficeId {

  def clerkClass: Class[E] =
    implicitly[ClassTag[E]].runtimeClass.asInstanceOf[Class[E]]
}

class Office(val officeId: OfficeId, val actor: ActorRef) {
  def id: EntityId = officeId.id
  def department: String = officeId.department
  def actorPath: ActorPath = actor.path

  def deliver(msg: Any)(implicit dh: DeliveryHandler): Unit = {
    dh((actorPath, msg))
  }

  def !!(msg: Any)(implicit dh: DeliveryHandler): Unit = deliver(msg)
}