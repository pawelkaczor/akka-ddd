package pl.newicom.dddd.scheduling

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.coordination.{ReceptorBuilder, ReceptorConfig}

object DeadlinesReceptor {

  def apply(businessUnit: EntityId, department: String): ReceptorConfig =
    ReceptorBuilder("DeadlinesProcess")
      .reactTo(currentDeadlinesOfficeId(department).caseRef(businessUnit))
      .autoRoute
      .copy(isSupporting_MustFollow_Attribute = false)
}