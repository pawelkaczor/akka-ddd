package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.pattern.pipe
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink}
import eventstore._
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.view.ViewUpdateInitializer.ViewUpdateInitException
import pl.newicom.dddd.view.ViewUpdateService._
import pl.newicom.eventstore.EventstoreSubscriber

import scala.concurrent.{ExecutionContext, Future}

object ViewUpdateService {
  object EnsureViewStoreAvailable

  case class InitiateViewUpdate(esCon: EsConnection)
  case class ViewUpdateInitiated(esCon: EsConnection)

  case class ViewUpdateConfigured(viewUpdate: ViewUpdate)

  case class ViewUpdate(office: BusinessEntity, lastEventNr: Option[Long], runnable: RunnableGraph[Future[Unit]]) {
    override def toString =  s"ViewUpdate(officeId = ${office.id}, lastEventNr = $lastEventNr)"
  }

}

abstract class ViewUpdateService extends Actor with EventstoreSubscriber with ActorLogging {

  type VUConfig <: ViewUpdateConfig

  implicit val ec: ExecutionContext = context.dispatcher

  def vuConfigs: Seq[VUConfig]

  def viewHandler(config: VUConfig): ViewHandler

  def ensureViewStoreAvailable: Future[Unit]

  /**
   * Overridable initialization logic
   */
  def onViewUpdateInit(esCon: EsConnection): Future[ViewUpdateInitiated] =
    Future.successful(ViewUpdateInitiated(esCon))

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
    case InitiateViewUpdate(esCon) =>
      onViewUpdateInit(esCon) pipeTo self

    case ViewUpdateInitiated(esCon) =>
      log.debug("Initiated.")
      vuConfigs.map(viewUpdate(esCon, _)).foreach(_.pipeTo(self))

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


  def viewUpdate(esCon: EsConnection, vuConfig: VUConfig): Future[ViewUpdate] = {
    val handler = viewHandler(vuConfig)
    val office = vuConfig.office
    handler.lastEventNumber.map { lastEvtNrOpt =>
      ViewUpdate(office, lastEvtNrOpt,
        eventSource(esCon, office, lastEvtNrOpt)
          .mapAsync(1) {
            msgRecord => handler.handle(msgRecord.msg, msgRecord.position)
          }.toMat(Sink.ignore)(Keep.right)
      )
    }
  }

}
