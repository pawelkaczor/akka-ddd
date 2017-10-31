package pl.newicom.dddd.office

import pl.newicom.dddd.{BusinessEntity, Eventsourced}
import pl.newicom.dddd.aggregate.{Command, EntityId}
import pl.newicom.dddd.cluster.DefaultDistributionStrategy

import scala.reflect.ClassTag

trait OfficeId extends BusinessEntity with Eventsourced {

  def caseRef(caseLocalId: EntityId): CaseRef =
    CaseRef(s"$id-$caseLocalId", this, version = None)

  def distributionStrategy = new DefaultDistributionStrategy
}

case class CaseRef(id: EntityId, responsible: Eventsourced, version: Option[Long]) extends BusinessEntity with Eventsourced {
  def department: String = responsible.department
  def localId: EntityId = if (id.contains('-')) id.split('-').last else id
}

case class RemoteOfficeId[M: ClassTag](id: EntityId, department: String, messageClass: Class[M]) extends OfficeId {
  def handles(command: Command): Boolean = messageClass.isAssignableFrom(command.getClass)
}
