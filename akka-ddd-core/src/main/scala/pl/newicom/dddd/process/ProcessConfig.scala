package pl.newicom.dddd.process

import pl.newicom.dddd.aggregate.{DomainEvent, EntityId}
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.process.ProcessConfig.CorrelationIdResolver
import pl.newicom.dddd.saga.BusinessProcessId

import scala.reflect.ClassTag

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
