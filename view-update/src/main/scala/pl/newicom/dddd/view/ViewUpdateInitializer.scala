package pl.newicom.dddd.view

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import eventstore.EventStoreExtension
import eventstore.EventStream.System
import pl.newicom.dddd.view.ViewUpdateInitializer._

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble
import scala.util.{Failure, Success}

object ViewUpdateInitializer {
  object Started
  class ViewUpdatingInitializationException(cause: Throwable) extends Exception(cause)
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

  override def receive: Receive = {
    case Started =>
      Future.sequence(List(ensureEventStoreAvailable(), ensureViewStoreAvailable())).onComplete {
        case Success(_) => updateService ! ViewUpdateService.Start(esExtension.actor)
        case Failure(ex) => throw new ViewUpdatingInitializationException(ex)
      }
  }

}
