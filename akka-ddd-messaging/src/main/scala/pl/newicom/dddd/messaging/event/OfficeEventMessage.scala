package pl.newicom.dddd.messaging.event

import pl.newicom.dddd.aggregate.DomainEvent
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.office.CaseRef

object OfficeEventMessage {
  def apply(em: EventMessage, caseRef: CaseRef): OfficeEventMessage =
    OfficeEventMessage(caseRef, em.event, em.metadata)

  def apply(caseRef: CaseRef, event: DomainEvent): OfficeEventMessage =
    OfficeEventMessage(caseRef, event, MetaData.initial)
}

case class OfficeEventMessage(caseRef: CaseRef, event: DomainEvent, metadata: MetaData) extends EventMessage {

  override type MessageImpl = OfficeEventMessage

  override protected def withNewMetaData(m: MetaData): OfficeEventMessage =
    copy(metadata = m)

  override def toString: String = {
    s"OfficeEventMessage(event = $event, metaData = $metadata)"
  }

}
