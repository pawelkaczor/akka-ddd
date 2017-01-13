package pl.newicom.dddd.scheduling

import akka.actor.ActorPath
import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.{Command, DomainEvent, EntityId}

  //
  // Commands
  //
  case class ScheduleEvent(businessUnit: String, target: ActorPath, deadline: DateTime, event: DomainEvent) extends Command {
    def aggregateId: EntityId = businessUnit
  }

  // 
  // Events
  //
  sealed trait SchedulerEvent

  case class ScheduledEventMetadata(businessUnit: String, target: ActorPath, deadline: DateTime, deadlineMillis: Long) extends SchedulerEvent

  case class EventScheduled(metadata: ScheduledEventMetadata, event: DomainEvent) extends SchedulerEvent

