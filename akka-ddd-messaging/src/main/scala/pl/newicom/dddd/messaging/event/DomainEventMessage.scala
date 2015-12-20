package pl.newicom.dddd.messaging.event

import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.DomainEvent
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.utils.UUIDSupport._


object DomainEventMessage {
  def apply(em: EventMessage, snapshotId: AggregateSnapshotId): DomainEventMessage =
    DomainEventMessage(snapshotId, em.event, em.id, em.timestamp, em.metadata)
}

case class DomainEventMessage(
    snapshotId: AggregateSnapshotId,
    event: DomainEvent,
    id: String = uuid,
    timestamp: DateTime = new DateTime,
    metadata: Option[MetaData] = None)
  extends EventMessage {

  override type MessageImpl = DomainEventMessage

  override def copyWithMetaData(m: Option[MetaData]): DomainEventMessage =
    copy(metadata = m)

  def aggregateId = snapshotId.aggregateId

  def sequenceNr = snapshotId.sequenceNr

  override def toString: String = {
    s"DomainEventMessage(event = $event, id = $id, timestamp = $timestamp, metaData = $metadata)"
  }

}