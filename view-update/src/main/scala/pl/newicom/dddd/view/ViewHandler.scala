package pl.newicom.dddd.view

import pl.newicom.dddd.messaging.event.DomainEventMessage

abstract class ViewHandler(val vuConfig: ViewUpdateConfig) {

  def handle(eventMessage: DomainEventMessage, eventNumber: Long)

  def lastEventNumber: Option[Long]

  protected def viewName = vuConfig.viewName

}
