package pl.newicom.dddd.messaging

import pl.newicom.dddd.messaging.MetaAttribute._

object MetaDataPropagationPolicy {
  val onCommandAccepted: OnCommandAccepted.type     = OnCommandAccepted
  val onCommandSentByPM: OnCommandSentByPM.type     = OnCommandSentByPM
  val onEventAcceptedByPM: OnEventAcceptedByPM.type = OnEventAcceptedByPM
}

sealed trait MetaDataPropagationPolicy {
  def apply(from: MetaData, to: MetaData): MetaData
}

case object OnCommandAccepted extends MetaDataPropagationPolicy {
  override def apply(commandAttrs: MetaData, eventAttrs: MetaData): MetaData = {
    eventAttrs
      .withAttr(Causation_Id, commandAttrs.get(Id))
      .withOptionalAttr(Correlation_Id, commandAttrs.tryGet(Correlation_Id))
      .withAttr(Tags, Tags.merge(commandAttrs, eventAttrs))
  }
}

case object OnCommandSentByPM extends MetaDataPropagationPolicy {
  override def apply(eventAttrs: MetaData, commandAttrs: MetaData): MetaData = {
    commandAttrs
      .withAttr(Causation_Id, eventAttrs.get(Id))
      .withOptionalAttr(Correlation_Id, eventAttrs.tryGet(Correlation_Id))
      .withAttr(Tags, Tags.merge(eventAttrs, commandAttrs))
  }
}

case object OnEventAcceptedByPM extends MetaDataPropagationPolicy {
  override def apply(receivedAttrs: MetaData, targetAttrs: MetaData): MetaData = {
    targetAttrs
      .withAttr(Delivery_Id, receivedAttrs.get(Delivery_Id))
      .withOptionalAttr(Must_Follow, receivedAttrs.tryGet(Must_Follow))
      .withAttr(Causation_Id, receivedAttrs.get(Id))
      .withOptionalAttr(Correlation_Id, receivedAttrs.tryGet(Correlation_Id))
      .remove(Tags)
  }
}
