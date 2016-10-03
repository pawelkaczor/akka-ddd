package pl.newicom.dddd.scheduling

import akka.actor.ActorPath
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.messaging.event.{EventStreamSubscriber, EventMessage}
import pl.newicom.dddd.process.{Receptor, ReceptorBuilder, ReceptorConfig}

object DeadlinesReceptor {
  def apply(businessUnit: EntityId): ReceptorConfig = ReceptorBuilder()
    .reactTo(CurrentDeadlinesOfficeId.clerk(businessUnit))
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

abstract class DeadlinesReceptor extends Receptor {
  this: EventStreamSubscriber =>

  override def isSupporting_MustFollow_Attribute: Boolean = false

}