package pl.newicom.dddd.scheduling

import akka.actor.Props
import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.test.support.TestConfig.testSystem
import SchedulerSpec._
import pl.newicom.dddd.actor.DefaultConfig
import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.test.ar.ARSpec

object SchedulerSpec {
  val businessUnit = "test"

  implicit val schedulingOfficeID: LocalOfficeId[Scheduler] = schedulingLocalOfficeId("Scheduling")

  implicit def actorFactory: AggregateRootActorFactory[Scheduler] =
    AggregateRootActorFactory[Scheduler](pc => Props(new Scheduler(DefaultConfig(pc))))

}

class SchedulerSpec extends ARSpec[SchedulerEvent, Scheduler](Some(testSystem)) {

  "Scheduling office" should {
    "schedule event" in {
      when {
        ScheduleEvent(businessUnit, null, DateTime.now().plusMinutes(1), null)
      }
      .expect { c =>
        EventScheduled(
          ScheduledEventMetadata(
            c.businessUnit,
            c.target,
            c.deadline.withSecondOfMinute(0).withMillisOfSecond(0),
            c.deadline.getMillis),
          c.event)
      }
    }
  }

}
