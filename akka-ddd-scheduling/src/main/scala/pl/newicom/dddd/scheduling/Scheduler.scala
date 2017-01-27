package pl.newicom.dddd.scheduling

import akka.persistence.Recovery
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.scheduling.Scheduler.State

object Scheduler extends AggregateRootSupport[SchedulerEvent] {

  //
  // State
  //
  sealed trait State extends AggregateState[State] {
    override def apply: StateMachine = apply(this)

    def apply(result: State): StateMachine = {
      case EventScheduled(_, _) => result
    }
  }

  implicit case object Uninitialized extends State with Uninitialized[State] {
    override def apply: StateMachine = apply(result = new State {})
  }

}

class Scheduler(val pc: PassivationConfig)(implicit val officeID: LocalOfficeId[Scheduler]) extends AggregateRoot[SchedulerEvent, State, Scheduler] {
  this: EventPublisher =>

  // Skip recovery
  override def recovery = Recovery(toSequenceNr = 0L)

  // Disable automated recovery on restart
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = ()

  override def handleCommand: HandleCommand = {
    case ScheduleEvent(_, target, deadline, event) =>
      val metadata = ScheduledEventMetadata(
        businessUnit = id,
        target,
        deadline.withSecondOfMinute(0).withMillisOfSecond(0),
        deadline.getMillis)

      EventScheduled(metadata, event)
  }
}
