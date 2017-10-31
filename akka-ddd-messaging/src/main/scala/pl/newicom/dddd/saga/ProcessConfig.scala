package pl.newicom.dddd.saga

import pl.newicom.dddd.{BusinessEntity, Eventsourced}
import pl.newicom.dddd.aggregate.{DomainEvent, EntityId}
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.saga.ProcessConfig._

import scala.reflect.ClassTag

case class BusinessProcessDomain(id: EntityId, department: String) extends BusinessEntity with Eventsourced

case class BusinessProcessId(processDomain: String, processId: EntityId, department: String = null) extends BusinessEntity with Eventsourced {
  def id: EntityId = processId

  val domain: Eventsourced = BusinessProcessDomain(processDomain, department)
}

object ProcessConfig {
  type CorrelationIdResolver = PartialFunction[DomainEvent, EntityId]
}

abstract class ProcessConfig[E : ClassTag](val process: BusinessProcessId)
  extends LocalOfficeId[E](process.processId, Option(process.department).getOrElse(process.processDomain)) {

  /**
    * Correlation ID identifies process instance.
    */
  def correlationIdResolver: CorrelationIdResolver

}
