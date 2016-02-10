package pl.newicom.dddd.view

import akka.Done
import pl.newicom.dddd.messaging.event.OfficeEventMessage
import scala.concurrent.Future

abstract class ViewHandler(val vuConfig: ViewUpdateConfig) {

  def handle(eventMessage: OfficeEventMessage, eventNumber: Long): Future[Done]

  def lastEventNumber: Future[Option[Long]]

  protected def viewName = vuConfig.viewName

}
