package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import pl.newicom.dddd.view.ViewUpdateInitializer._
import pl.newicom.dddd.view.ViewUpdateService.{EnsureViewStoreAvailable, EnsureEventStoreAvailable}

import scala.concurrent.duration._
import akka.pattern.ask
import akka.pattern.pipe

object ViewUpdateInitializer {
  case object InitializationStarted
  case object InitializationCompleted
  class ViewUpdateInitException(cause: Throwable) extends Exception(cause)
}

class ViewUpdateInitializer(updateService: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  implicit val timeout = Timeout(5.seconds)

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    self ! InitializationStarted
  }

  override def receive: Receive = {
    case InitializationStarted =>
      (for {
        _ <- updateService ? EnsureEventStoreAvailable
        _ <- updateService ? EnsureViewStoreAvailable
      } yield InitializationCompleted).pipeTo(self)

    case InitializationCompleted =>
      updateService ! ViewUpdateService.InitiateViewUpdate

    case Failure(ex) =>
      throw new ViewUpdateInitException(ex)


  }

}
