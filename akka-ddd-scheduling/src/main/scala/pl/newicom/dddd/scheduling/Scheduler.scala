package pl.newicom.dddd.scheduling

import akka.persistence.Recovery
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.scheduling.Scheduler.State

object Scheduler extends AggregateRootSupport {

  //
  // State
  //
  sealed trait State extends AggregateState[State]

  implicit case object SchedulerState extends State with Uninitialized[State] with Initialized[State] {
    override def apply: StateMachine = {
      case EventScheduled(_, _) => this
    }
  }

}

class Scheduler(val pc: PassivationConfig)(implicit val officeID: LocalOfficeId[Scheduler]) extends AggregateRoot[State, Scheduler] {
  this: EventPublisher =>

  // Skip recovery
  override def recovery = Recovery(toSequenceNr = 0L)

  // Disable automated recovery on restart
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = ()

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
