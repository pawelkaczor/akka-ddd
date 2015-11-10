package pl.newicom.dddd.scheduling

import akka.actor.ActorPath
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.process.{ReceptorBuilder, ReceptorConfig}

object DeadlinesReceptor {
  def apply(businessUnit: String): ReceptorConfig = ReceptorBuilder()
    .reactToStream(currentDeadlinesStream(businessUnit))
    .applyTransduction {
      case em @ EventMessage(_, EventScheduled(metadata, event)) =>
        EventMessage(event)
          .withCorrelationId(em.correlationId.get)
          .withMetaAttribute("target", metadata.target.toSerializationFormat)
    }
    .route {
      case em: EventMessage =>
        ActorPath.fromString(em.getMetaAttribute("target"))
    }
}
