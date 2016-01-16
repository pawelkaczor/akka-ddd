package pl.newicom.dddd.messaging.event

import pl.newicom.dddd.aggregate.EntityId

case class CaseId(entityId: EntityId, version: Long = 0)
