package pl.newicom.dddd

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.office.RemoteOfficeId

package object process {

  def commandQueue(department: String): Eventsourced = {
    val dep = department
    new BusinessEntity with Eventsourced {
      def id: EntityId = s"command-queue-$department"
      def department: String = dep
    }
  }

  def commandSinkOfficeId(department: String): RemoteOfficeId[EnqueueCommand] =
    RemoteOfficeId(commandSinkName(department), department, classOf[EnqueueCommand])

  def commandSinkName(department: String) = s"command-sink-$department"

}
