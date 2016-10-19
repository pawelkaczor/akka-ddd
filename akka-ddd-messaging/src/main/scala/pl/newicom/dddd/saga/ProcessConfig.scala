package pl.newicom.dddd.saga

import pl.newicom.dddd.aggregate.{DomainEvent, EntityId}
import pl.newicom.dddd.office.LocalOfficeId

import scala.reflect.ClassTag
import ProcessConfig._

object ProcessConfig {
  val OfficeIdSuffix = "Saga"
  type CorrelationIdResolver = PartialFunction[DomainEvent, EntityId]
}

/**
  * @param bpsName name of Business Process Stream (bps)
  */
abstract class ProcessConfig[E : ClassTag](val bpsName: String, departmentId: EntityId = null)
  extends LocalOfficeId[E](s"$bpsName$OfficeIdSuffix", Option(departmentId).getOrElse(bpsName)) {

  /**
    * Correlation ID identifies process instance.
    */
  def correlationIdResolver: CorrelationIdResolver

}
