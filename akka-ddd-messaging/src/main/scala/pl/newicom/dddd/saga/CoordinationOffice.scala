package pl.newicom.dddd.saga

import akka.actor.ActorRef
import pl.newicom.dddd.coordination.{ReceptorBuilder, ReceptorConfig}
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.Office
import pl.newicom.dddd.saga.ProcessConfig.CorrelationIdResolver

import scala.reflect.ClassTag

class CoordinationOffice[E: ClassTag](val config: ProcessConfig[E], actor: ActorRef) extends Office(config, actor) {

  def receptorConfig: ReceptorConfig =
    ReceptorBuilder(id = config.process.id)
      .reactTo(config.process.domain)
      .applyTransduction {
        case em @ EventMessage(_, event) if correlationIdResolver.isDefinedAt(event) =>
          em.withCorrelationId(correlationIdResolver(event))
      }
      .propagateTo(actorPath)

  def correlationIdResolver: CorrelationIdResolver =
    config.correlationIdResolver

}
