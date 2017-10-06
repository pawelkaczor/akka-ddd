package pl.newicom.dddd.scheduling

import akka.actor.ActorPath
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.coordination.{ReceptorBuilder, ReceptorConfig}
import pl.newicom.dddd.messaging.MetaAttribute.Target
import pl.newicom.dddd.messaging.event.EventMessage

object DeadlinesReceptor {

  def apply(businessUnit: EntityId, department: String): ReceptorConfig =
    ReceptorBuilder("DeadlinesProcess")
      .reactTo(currentDeadlinesOfficeId(department).caseRef(businessUnit))
      .route {
        case em: EventMessage =>
          ActorPath.fromString(em.getMetaAttribute(Target))
      }
      .copy(isSupporting_MustFollow_Attribute = false)
}