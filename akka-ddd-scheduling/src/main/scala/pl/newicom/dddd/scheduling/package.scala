package pl.newicom.dddd

import pl.newicom.dddd.office.LocalOfficeId.fromRemoteId
import pl.newicom.dddd.office.RemoteOfficeId

package object scheduling {

  def schedulingOfficeId(department: String) = RemoteOfficeId("deadlines", department, classOf[ScheduleEvent])

  def schedulingLocalOfficeId(department: String) = fromRemoteId[Scheduler](schedulingOfficeId(department))

  def currentDeadlinesOfficeId(department: String) = RemoteOfficeId("currentDeadlines", department, classOf[ScheduleEvent])

}