package pl.newicom.dddd.office

import akka.actor.{ActorPath, ActorRef}
import pl.newicom.dddd.aggregate.{BusinessEntity, EntityId}

import scala.reflect.ClassTag

trait OfficeId extends BusinessEntity {
  def clerkGlobalId(clerkId: EntityId) = s"$id-$clerkId"

  def clerk(clerkId: EntityId): BusinessEntity = new BusinessEntity {
    def id = clerkGlobalId(clerkId)
  }
}

case class RemoteOfficeId(id: EntityId) extends OfficeId

object LocalOfficeId {
  implicit def fromRemoteId[E: ClassTag](remoteId: RemoteOfficeId): LocalOfficeId[E] = LocalOfficeId[E](remoteId.id)
}

case class LocalOfficeId[E: ClassTag](val id: EntityId) extends OfficeId {
  def clerkClass: Class[E] = implicitly[ClassTag[E]].runtimeClass.asInstanceOf[Class[E]]
}

case class Office[E: ClassTag](val officeId: LocalOfficeId[E], actor: ActorRef) {
  def id: EntityId = officeId.id
  def actorPath: ActorPath = actor.path
}


class SagaOffice[E: ClassTag](val config: SagaConfig[E], actor: ActorRef) extends Office[E](config, actor) {
  def bps = new BusinessEntity {
    override def id: EntityId = config.bpsName
  }
}