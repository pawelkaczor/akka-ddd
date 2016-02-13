package pl.newicom.dddd.office

import pl.newicom.dddd.aggregate.{DomainEvent, EntityId}

import scala.reflect.ClassTag

/**
  * @param bpsName name of Business Process Stream (bps)
  */
abstract class SagaConfig[E : ClassTag](val bpsName: String, departmentId: EntityId = null)
  extends LocalOfficeId[E](s"${bpsName}Saga", Option(departmentId).getOrElse(bpsName)) {

  /**
    * Correlation ID identifies process instance.
    */
  def correlationIdResolver: PartialFunction[DomainEvent, EntityId]

}
