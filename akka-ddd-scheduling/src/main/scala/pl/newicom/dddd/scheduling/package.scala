package pl.newicom.dddd

import pl.newicom.dddd.messaging.event.ClerkEventStream
import pl.newicom.dddd.office.OfficeInfo

package object scheduling {

  implicit val schedulingOffice: OfficeInfo[SchedulingOffice] = new OfficeInfo[SchedulingOffice] {
    def name: String = "deadlines"
    def serializationHints = Scheduler.serializationHints
  }

  def currentDeadlinesStream(businessUnit: String) = ClerkEventStream("currentDeadlines", businessUnit)
}
