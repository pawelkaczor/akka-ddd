package pl.newicom.dddd.view

import pl.newicom.dddd.messaging.event.DomainEventMessage

import scala.concurrent.Future

abstract class ViewHandler(val vuConfig: ViewUpdateConfig) {

  def handle(eventMessage: DomainEventMessage, eventNumber: Long): Future[Unit]

  def lastEventNumber: Future[Option[Long]]

  protected def viewName = vuConfig.viewName

}
