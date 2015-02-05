package pl.newicom.dddd.messaging.event

import java.util.UUID

import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.DomainEvent

case class DomainEventMessage(
    snapshotId: AggregateSnapshotId,
    override val event: DomainEvent,
    override val identifier: String = UUID.randomUUID().toString,
    override val timestamp: DateTime = new DateTime)
  extends EventMessage(event, identifier, timestamp) {

  override def entityId = aggregateId

  def this(em: EventMessage, s: AggregateSnapshotId) = this(s, em.event, em.identifier, em.timestamp)

  def aggregateId = snapshotId.aggregateId

  def sequenceNr = snapshotId.sequenceNr

}