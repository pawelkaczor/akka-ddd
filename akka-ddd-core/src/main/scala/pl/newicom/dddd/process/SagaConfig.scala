package pl.newicom.dddd.process

import pl.newicom.dddd.aggregate.{EntityId, DomainEvent}
import pl.newicom.dddd.office.OfficeInfo

/**
 * @param bpsName name of Business Process Stream (bps)
 */
abstract class SagaConfig[A <: Saga](val bpsName: String) extends OfficeInfo[A] {

  def name = bpsName

  /**
   * Correlation ID identifies process instance. It is used to route EventMessage
   * messages created by [[SagaManager]] to [[Saga]] instance,
   */
  def correlationIdResolver: PartialFunction[DomainEvent, EntityId]

}
