package pl.newicom.dddd

import pl.newicom.dddd.office.{LocalOfficeId, RemoteOfficeId}

package object scheduling {

  implicit val schedulingOfficeId: LocalOfficeId[Scheduler] = new LocalOfficeId[Scheduler]("deadlines")

  val currentDeadlinesOfficeId = RemoteOfficeId("currentDeadlines")

}