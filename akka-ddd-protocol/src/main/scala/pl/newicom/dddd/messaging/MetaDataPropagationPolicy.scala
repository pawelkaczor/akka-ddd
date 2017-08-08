package pl.newicom.dddd.messaging

import pl.newicom.dddd.messaging.MetaAttribute._

object MetaDataPropagationPolicy {
  val onCommandAccepted: OnCommandAcceptedByAR.type     = OnCommandAcceptedByAR
  val onCommandSentByPM: OnCommandDeliveryRequestedByPM.type     = OnCommandDeliveryRequestedByPM
  val onEventAcceptedByPM: OnEventAcceptedByPM.type = OnEventAcceptedByPM
}

sealed trait MetaDataPropagationPolicy {
  def apply(from: MetaData, to: MetaData): MetaData
}

case object OnCommandAcceptedByAR extends MetaDataPropagationPolicy {
  override def apply(command: MetaData, event: MetaData): MetaData = {
    event
      .withAttr(Causation_Id, command.get(Id))
      .withOptionalAttr(Correlation_Id, command.tryGet(Correlation_Id))
      .withAttr(Tags, Tags.merge(command, event))
  }
}

case object OnEventAcceptedByPM extends MetaDataPropagationPolicy {
  override def apply(receivedEvent: MetaData, eventToStore: MetaData): MetaData = {
    eventToStore
      .withAttr(Delivery_Id, receivedEvent.get(Delivery_Id))
      .withOptionalAttr(Must_Follow, receivedEvent.tryGet(Must_Follow))
      .withAttr(Causation_Id, receivedEvent.get(Id))
      .withOptionalAttr(Correlation_Id, receivedEvent.tryGet(Correlation_Id))
      .remove(Tags)
  }
}

case object OnCommandDeliveryRequestedByPM extends MetaDataPropagationPolicy {
  override def apply(event: MetaData, command: MetaData): MetaData = {
    command
      .withAttr(Causation_Id, event.get(Id))
      .withOptionalAttr(Correlation_Id, event.tryGet(Correlation_Id))
      .withAttr(Tags, Tags.merge(event, command))
  }
}
