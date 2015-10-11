package pl.newicom.dddd.scheduling

import akka.persistence.Recovery
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.scheduling.Scheduler.SchedulerState

object Scheduler {

  //
  // State
  //
  case class SchedulerState() extends AggregateState {
    override def apply = {
      case e: EventScheduled => this
    }
  }
}

class Scheduler(val pc: PassivationConfig, businessUnit: String) extends AggregateRoot[SchedulerState] {
  this: EventPublisher =>

  override def persistenceId = s"${schedulingOffice.name}-$businessUnit"

  // Skip recovery
  override def recovery = Recovery(toSequenceNr = 0L)

  // Disable automated recovery on restart
  override def preRestart(reason: Throwable, message: Option[Any]) = ()

  override val factory: AggregateRootFactory = {
    case EventScheduled(_, _, _, _, _) => SchedulerState()
  }

  override def handleCommand: Receive = {
    case ScheduleEvent(_, target, deadline, msg) =>
      raise(
        EventScheduled(
          businessUnit,
          target,
          deadline.withSecondOfMinute(0).withMillisOfSecond(0),
          deadline.getMillis,
          msg)
      )
  }
}
