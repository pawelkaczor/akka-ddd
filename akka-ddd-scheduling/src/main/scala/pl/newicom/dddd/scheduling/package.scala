package pl.newicom.dddd

import pl.newicom.dddd.office.{LocalOfficeId, RemoteOfficeId}

package object scheduling {

  def schedulingOfficeId(department: String) = new LocalOfficeId[Scheduler]("deadlines", department)

  def currentDeadlinesOfficeId(department: String) = RemoteOfficeId("currentDeadlines", department, classOf[ScheduleEvent])

}