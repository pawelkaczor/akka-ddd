package pl.newicom.dddd.messaging.event

import pl.newicom.dddd.aggregate.BusinessEntity.EntityId

case class AggregateSnapshotId(aggregateId: EntityId, sequenceNr: Long = 0)
