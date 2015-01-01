package pl.newicom.dddd.view

import pl.newicom.dddd.messaging.event.DomainEventMessage

abstract class ViewHandler(val vuConfig: ViewUpdateConfig) {

  def handle(eventMessage: DomainEventMessage, eventNumber: Int)

  def lastEventNumber: Int

  protected def viewName = vuConfig.viewName

}
