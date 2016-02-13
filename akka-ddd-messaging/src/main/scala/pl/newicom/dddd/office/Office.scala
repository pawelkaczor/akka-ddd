package pl.newicom.dddd.office

import akka.actor.{ActorPath, ActorRef}
import pl.newicom.dddd.aggregate.{BusinessEntity, EntityId}

import scala.reflect.ClassTag

trait OfficeId extends BusinessEntity {
  def clerkGlobalId(clerkId: EntityId): String =
    s"$id-$clerkId"

  def clerk(clerkId: EntityId): BusinessEntity =
    Clerk(clerkGlobalId(clerkId), department)
}

case class Clerk(id: EntityId, department: String) extends BusinessEntity

case class RemoteOfficeId(id: EntityId, department: String) extends OfficeId

object LocalOfficeId {
  implicit def fromRemoteId[E: ClassTag](remoteId: RemoteOfficeId): LocalOfficeId[E] = LocalOfficeId[E](remoteId.id, remoteId.department)
}

case class LocalOfficeId[E: ClassTag](val id: EntityId, val department: String) extends OfficeId {
  def clerkClass: Class[E] = implicitly[ClassTag[E]].runtimeClass.asInstanceOf[Class[E]]
}

case class Office[E: ClassTag](val officeId: LocalOfficeId[E], actor: ActorRef) {
  def id: EntityId = officeId.id
  def actorPath: ActorPath = actor.path
}



