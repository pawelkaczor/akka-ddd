package pl.newicom.dddd.messaging.event

import java.util.{Date, UUID}

import pl.newicom.dddd.aggregate.DomainEvent
import pl.newicom.dddd.messaging.EntityMessage
import pl.newicom.dddd.messaging.Message._

case class DomainEventMessage(
    snapshotId: AggregateSnapshotId,
    override val event: DomainEvent,
    override val identifier: String = UUID.randomUUID().toString,
    override val timestamp: Date = new Date,
    override val metaData: Option[MetaData] = None)
  extends EventMessage(event, identifier, timestamp, metaData) with EntityMessage {

  override def entityId = aggregateId

  def this(em: EventMessage, s: AggregateSnapshotId) = this(s, em.event, em.identifier, em.timestamp, em.metaData)

  def aggregateId = snapshotId.aggregateId

  def sequenceNr = snapshotId.sequenceNr

  override def payload: Any = event

}