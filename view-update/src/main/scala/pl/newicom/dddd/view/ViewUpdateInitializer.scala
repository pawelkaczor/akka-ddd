package pl.newicom.dddd.view

import akka.actor.Status.{Success, Failure}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import eventstore.EventStoreExtension
import eventstore.EventStream.System
import pl.newicom.dddd.view.ViewUpdateInitializer._

import scala.concurrent.Future
import scala.concurrent.duration._

object ViewUpdateInitializer {
  object Started
  class ViewUpdateInitException(cause: Throwable) extends Exception(cause)
}

class ViewUpdateInitializer(updateService: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    self ! Started
  }

  private val esExtension: EventStoreExtension = EventStoreExtension(context.system)

  def ensureEventStoreAvailable(): Future[Any] = {
    esExtension.connection.getStreamMetadata(System("test"))
  }

  def ensureViewStoreAvailable(): Future[Any] = {
    import akka.pattern.ask
    implicit val timeout = Timeout(5.seconds)
    updateService ? ViewUpdateService.EnsureViewStoreAvailable
  }

  import akka.pattern.pipe
  override def receive: Receive = {
    case Started =>
      (for {
        _ <- ensureEventStoreAvailable()
        _ <- ensureViewStoreAvailable()
      } yield "OK").pipeTo(self)

    case Failure(ex) =>
      throw new ViewUpdateInitException(ex)

    case _ =>
      updateService ! ViewUpdateService.InitiateViewUpdate(esExtension.connection)

  }

}
