package pl.newicom.eventstore

import eventstore.EventStream.{Plain, System}
import eventstore.{EventStream => ESEventStream}
import pl.newicom.dddd.messaging.event.{ClerkEventStream, EventStream, OfficeEventStream}

object StreamNameResolver {

  def streamId(stream: EventStream): ESEventStream.Id = stream match {

    case OfficeEventStream(officeInfo) if officeInfo.isSagaOffice =>
      Plain(s"${stream.officeName}")

    case OfficeEventStream(officeInfo) =>
      System(s"ce-${stream.officeName}")

    case ClerkEventStream(officeName, clerkId) =>
      Plain(s"$officeName-$clerkId")
  }
}
