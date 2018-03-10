package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor, pipe}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink}
import pl.newicom.dddd.messaging.event.EventSourceProvider
import pl.newicom.dddd.view.ViewUpdateInitializer.ViewUpdateInitException
import pl.newicom.dddd.view.ViewUpdateService._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import akka.Done
import pl.newicom.dddd.Eventsourced

object ViewUpdateService {
  object EnsureViewStoreAvailable
  object EnsureEventStoreAvailable

  case object InitiateViewUpdate
  case object ViewUpdateInitiated
  case class ViewUpdateFailed(reason: Throwable)

  case class ViewUpdateConfigured(viewUpdate: ViewUpdate)

  case class ViewUpdate(eventsource: Eventsourced, lastEventNr: Option[Long], runnable: RunnableGraph[Future[Done]]) {
    override def toString = s"ViewUpdate(officeId = ${eventsource.streamName}, lastEventNr = $lastEventNr)"
  }

}

abstract class ViewUpdateService extends Actor with ActorLogging { this: EventSourceProvider =>

  type VUConfig <: ViewUpdateConfig

  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  implicit val ec: ExecutionContext = context.dispatcher

  def vuConfigs: Seq[VUConfig]

  def viewHandler(config: VUConfig): ViewHandler

  def ensureViewStoreAvailable: Future[Unit]

  /**
    * Overridable initialization logic
    */
  def onViewUpdateInit: Future[ViewUpdateInitiated.type] =
    Future.successful(ViewUpdateInitiated)

  def onViewUpdateFailed(reason: Throwable): Future[ViewUpdateFailed] =
    Future.successful(ViewUpdateFailed(reason))

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    val initializerProps = Props(new ViewUpdateInitializer(self))
    val supervisor = BackoffSupervisor.props(
      Backoff
        .onFailure(
          initializerProps,
          childName = "ViewUpdateInitializer",
          minBackoff = 3 seconds,
          maxBackoff = 30.seconds,
          randomFactor = 0.1
        )
        .withAutoReset(10.seconds)
        .withSupervisorStrategy(OneForOneStrategy() {
          case _: ViewUpdateInitException => Restart
          case _                          => Escalate
        }))
    context.actorOf(supervisor)
  }

  override def receive: Receive = {
    case InitiateViewUpdate =>
      onViewUpdateInit pipeTo self

    case ViewUpdateInitiated =>
      log.debug("Initiated.")
      vuConfigs.map(viewUpdate(eventStore, _)).foreach(_.pipeTo(self))

    case vu @ ViewUpdate(_, _, runnable) =>
      log.debug(s"Starting: $vu")
      runnable.run() pipeTo self

    case EnsureViewStoreAvailable =>
      ensureViewStoreAvailable pipeTo sender()

    case EnsureEventStoreAvailable =>
      ensureEventStoreAvailable pipeTo sender()

    case Failure(ex) =>
      onViewUpdateFailed(ex) pipeTo self

    case ViewUpdateFailed(reason) =>
      throw reason

    case unexpected =>
      onViewUpdateFailed(new RuntimeException(s"Unexpected message received: $unexpected")) pipeTo self

  }

  def viewUpdate(eventStore: EventStore, vuConfig: VUConfig): Future[ViewUpdate] = {
    val handler = viewHandler(vuConfig)
    val observable  = vuConfig.eventSource
    handler.lastEventNumber.map { lastEvtNrOpt =>
      ViewUpdate(
        observable,
        lastEvtNrOpt,
        eventSource(eventStore, observable, lastEvtNrOpt)
          .mapAsync(1) { msgRecord =>
            handler.handle(msgRecord.msg, msgRecord.position)
          }
          .toMat(Sink.ignore)(Keep.right)
      )
    }
  }

}
