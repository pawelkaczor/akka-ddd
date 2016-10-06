package pl.newicom.dddd.scheduling

import akka.persistence.Recovery
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.scheduling.Scheduler.SchedulerState

object Scheduler extends AggregateRootSupport {

  //
  // State
  //
  case class SchedulerState() extends AggregateState[SchedulerState] {
    override def apply = {
      case e: EventScheduled => this
    }
  }
}

class Scheduler(val pc: PassivationConfig)(implicit val officeID: LocalOfficeId[Scheduler]) extends AggregateRoot[SchedulerState, Scheduler] {
  this: EventPublisher =>

  // Skip recovery
  override def recovery = Recovery(toSequenceNr = 0L)

  // Disable automated recovery on restart
  override def preRestart(reason: Throwable, message: Option[Any]) = ()

  override val factory: AggregateRootFactory = {
    case EventScheduled(_, _) => SchedulerState()
  }

  override def handleCommand: Receive = {
    case ScheduleEvent(_, target, deadline, event) =>
      val metadata = ScheduledEventMetadata(
        businessUnit = id,
        target,
        deadline.withSecondOfMinute(0).withMillisOfSecond(0),
        deadline.getMillis)

      raise(EventScheduled(metadata, event))
  }
}
