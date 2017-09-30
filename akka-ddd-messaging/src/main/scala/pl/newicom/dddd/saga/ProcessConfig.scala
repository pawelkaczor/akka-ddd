package pl.newicom.dddd.saga

import pl.newicom.dddd.aggregate.{BusinessEntity, DomainEvent, EntityId}
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.saga.ProcessConfig._
import pl.newicom.dddd.utils.UUIDSupport.uuid7

import scala.reflect.ClassTag

case class BusinessProcessId(processName: String, processInstanceId: EntityId = uuid7, department: String = null) extends BusinessEntity {
  def id: EntityId = processInstanceId

  def processClass: BusinessEntity = new BusinessEntity {
    def id: String = processName
    def department: String = BusinessProcessId.this.department
  }
}

object ProcessConfig {
  type CorrelationIdResolver = PartialFunction[DomainEvent, EntityId]
}

abstract class ProcessConfig[E : ClassTag](val process: BusinessProcessId)
  extends LocalOfficeId[E](process.processName, Option(process.department).getOrElse(process.processName)) {

  /**
    * Correlation ID identifies process instance.
    */
  def correlationIdResolver: CorrelationIdResolver

}
