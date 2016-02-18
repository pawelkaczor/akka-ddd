package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink}
import pl.newicom.dddd.messaging.event.EventSourceProvider

import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.view.ViewUpdateInitializer.ViewUpdateInitException
import pl.newicom.dddd.view.ViewUpdateService._
import scala.concurrent.{ExecutionContext, Future}
import akka.Done

object ViewUpdateService {
  object EnsureViewStoreAvailable

  case class InitiateViewUpdate[ES](eventStore: ES)
  case class ViewUpdateInitiated[ES](eventStore: ES)

  case class ViewUpdateConfigured(viewUpdate: ViewUpdate)

  case class ViewUpdate(office: BusinessEntity, lastEventNr: Option[Long], runnable: RunnableGraph[Future[Done]]) {
    override def toString =  s"ViewUpdate(officeId = ${office.id}, lastEventNr = $lastEventNr)"
  }

}

abstract class ViewUpdateService extends Actor with ActorLogging {
  this: EventSourceProvider =>

  type VUConfig <: ViewUpdateConfig

  implicit val actorMaterializer = ActorMaterializer()

  implicit val ec: ExecutionContext = context.dispatcher

  def vuConfigs: Seq[VUConfig]

  def viewHandler(config: VUConfig): ViewHandler

  def ensureViewStoreAvailable: Future[Unit]

  /**
   * Overridable initialization logic
   */
  def onViewUpdateInit(eventStore: EventStore): Future[ViewUpdateInitiated[EventStore]] =
    Future.successful(ViewUpdateInitiated(eventStore))

  /**
   * Restart ViewUpdateInitializer until it successfully obtains connection to event store and view store
   * During normal processing escalate all exceptions so that feeding is restarted
   */
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException               => Stop
    case _: ActorInitializationException       => Stop
    case _: ViewUpdateInitException            => Restart
    case _                                     => Escalate
  }


  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.actorOf(Props(new ViewUpdateInitializer(self)))
  }

  override def receive: Receive = {
    case InitiateViewUpdate(eventStore: EventStore @unchecked) =>
      onViewUpdateInit(eventStore) pipeTo self

    case ViewUpdateInitiated(eventStore: EventStore @unchecked) =>
      log.debug("Initiated.")
      vuConfigs.map(viewUpdate(eventStore, _)).foreach(_.pipeTo(self))

    case vu @ ViewUpdate(_, _, runnable) =>
        log.debug(s"Starting: $vu")
        runnable.run() pipeTo self

    case Failure(ex) =>
      throw ex

    case EnsureViewStoreAvailable =>
      ensureViewStoreAvailable pipeTo sender()

    case unexpected =>
      throw new RuntimeException(s"Unexpected message received: $unexpected")
  }


  def viewUpdate(eventStore: EventStore, vuConfig: VUConfig): Future[ViewUpdate] = {
    val handler = viewHandler(vuConfig)
    val office = vuConfig.office
    handler.lastEventNumber.map { lastEvtNrOpt =>
      ViewUpdate(office, lastEvtNrOpt,
        eventSource(eventStore, office, lastEvtNrOpt)
          .mapAsync(1) {
            msgRecord => handler.handle(msgRecord.msg, msgRecord.position)
          }.toMat(Sink.ignore)(Keep.right)
      )
    }
  }

}
