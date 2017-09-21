package pl.newicom.dddd.aggregate

import pl.newicom.dddd.messaging.MetaAttribute.{Publisher_Type, Tags}
import pl.newicom.dddd.messaging.PublisherTypeValue.AR
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.messaging.{MetaData, MetaDataPropagationPolicy}
import pl.newicom.dddd.office.OfficeId

trait EventMessageFactory {
  def toEventMessage(event: DomainEvent, source: OfficeId, causedBy: CommandMessage): EventMessage = {
    val commandAttrs = causedBy.metadata
    val eventAttrs = MetaData(
      Publisher_Type -> AR,
      Tags -> Set(source.id, source.department)
    )

    EventMessage(event).withMetaData(
      MetaDataPropagationPolicy.onCommandAccepted(commandAttrs, eventAttrs)
    )
  }

}
