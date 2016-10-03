package pl.newicom.dddd

import pl.newicom.dddd.office.{LocalOfficeId, RemoteOfficeId}

package object scheduling {

  implicit val schedulingOfficeId: LocalOfficeId[Scheduler] = new LocalOfficeId[Scheduler]("deadlines", "scheduling")

  implicit object CurrentDeadlinesOfficeId extends RemoteOfficeId("currentDeadlines", "scheduling", classOf[ScheduleEvent])

}