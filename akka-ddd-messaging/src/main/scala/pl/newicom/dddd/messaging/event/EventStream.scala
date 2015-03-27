package pl.newicom.dddd.messaging.event

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.office.OfficeInfo

sealed trait EventStream {
  def officeName: String
}

case class OfficeEventStream[A](officeInfo: OfficeInfo[A]) extends EventStream {
  def officeName = officeInfo.name
}

case class ClerkEventStream(officeName: String, clerkId: EntityId) extends EventStream