package pl.newicom.dddd.office

import pl.newicom.dddd.aggregate.{DomainEvent, EntityId}

import scala.reflect.ClassTag

/**
  * @param bpsName name of Business Process Stream (bps)
  */
abstract class SagaConfig[E : ClassTag](val bpsName: String) extends LocalOfficeId[E](s"${bpsName}Saga") {

  /**
    * Correlation ID identifies process instance.
    */
  def correlationIdResolver: PartialFunction[DomainEvent, EntityId]

}
