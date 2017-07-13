package pl.newicom.dddd.scheduling

import akka.persistence.Recovery
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.scheduling.Scheduler.SchedulerBehavior

object Scheduler extends AggregateRootSupport {

  sealed trait SchedulerBehavior extends AggregateActions[SchedulerEvent, SchedulerBehavior, Config]

  implicit case object Uninitialized extends SchedulerBehavior with Uninitialized[SchedulerBehavior] {
    def actions =
      withContext { ctx => handleCommand {
        case ScheduleEvent(_, target, deadline, event) =>
          val metadata = ScheduledEventMetadata(
            businessUnit = ctx.caseRef.localId,
            target,
            deadline.withSecondOfMinute(0).withMillisOfSecond(0),
            deadline.getMillis)

          EventScheduled(metadata, event)
      }}
      .handleEvent {
        case EventScheduled(_, _) => this
      }
  }

}

class Scheduler(override val config: Config)(implicit val officeID: LocalOfficeId[Scheduler])
  extends AggregateRoot[SchedulerEvent, SchedulerBehavior, Scheduler]
    with ConfigClass[Config] {

  // Skip recovery
  override def recovery = Recovery(toSequenceNr = 0L)

  // Disable automated recovery on restart
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = ()

}
