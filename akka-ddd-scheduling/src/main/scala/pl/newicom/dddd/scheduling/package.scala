package pl.newicom.dddd

import pl.newicom.dddd.office.LocalOfficeId.fromRemoteId
import pl.newicom.dddd.office.{LocalOfficeId, RemoteOfficeId}

package object scheduling {

  def schedulingOfficeId(department: String): RemoteOfficeId[ScheduleEvent] =
    RemoteOfficeId("deadlines", department, classOf[ScheduleEvent])

  def schedulingLocalOfficeId(department: String): LocalOfficeId[Scheduler] =
    fromRemoteId[Scheduler](schedulingOfficeId(department))

  def currentDeadlinesOfficeId(department: String): RemoteOfficeId[ScheduleEvent] =
    RemoteOfficeId("currentDeadlines", department, classOf[ScheduleEvent])

}