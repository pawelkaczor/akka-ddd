package pl.newicom.dddd.view

import akka.event.LoggingAdapter
import pl.newicom.dddd.aggregate.AggregateRoot.DomainEvent
import pl.newicom.dddd.messaging.event.{AggregateSnapshotId, DomainEventMessage}

abstract class AbstractProjectionSpec(implicit val log: LoggingAdapter) extends Function[DomainEventMessage, Unit] {

  def currentVersion(aggregateId: String): Option[Long] = None

  def isApplied(event: DomainEventMessage) =
    currentVersion(event.aggregateId).flatMap(v => Some(v >= event.sequenceNr)).getOrElse(false)

  override def apply(event: DomainEventMessage): Unit =
    if (!isApplied(event))
      apply(event.snapshotId, event.event)

  def apply(snapshotId: AggregateSnapshotId, event: DomainEvent)
}
