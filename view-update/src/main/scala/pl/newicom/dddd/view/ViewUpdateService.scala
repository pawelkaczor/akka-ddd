package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source, Sink, RunnableGraph}
import eventstore.EventNumber.Exact
import eventstore._
import pl.newicom.dddd.messaging.event.OfficeEventStream
import pl.newicom.dddd.office.OfficeInfo
import pl.newicom.dddd.view.ViewUpdateInitializer.ViewUpdateInitException
import pl.newicom.dddd.view.ViewUpdateService._
import pl.newicom.eventstore.{StreamNameResolver, EventstoreSerializationSupport}
import akka.pattern.pipe

import scala.concurrent.{ExecutionContext, Future}

object ViewUpdateService {
  object EnsureViewStoreAvailable

  case class InitiateViewUpdate(esCon: EsConnection)
  case class ViewUpdateInitiated(esCon: EsConnection)

  case class ViewUpdateConfigured(viewUpdate: ViewUpdate)

  object EventReceived {
    def apply(eventData: EventData, eventNr: Long, lastEventNrOpt: Option[Long]): EventReceived =
      EventReceived(eventData, eventNr, eventNr <= lastEventNrOpt.getOrElse(-1L))
  }

  case class EventReceived(eventData: EventData, eventNr: Long, alreadyProcessed: Boolean)

  case class ViewUpdate(officeInfo: OfficeInfo[_], lastEventNr: Option[Long], runnable: RunnableGraph[Future[Unit]]) {
    override def toString =  s"ViewUpdate(officeName = ${officeInfo.name}, lastEventNr = $lastEventNr)"
  }

}

abstract class ViewUpdateService extends Actor with EventstoreSerializationSupport with ActorLogging {

  type VUConfig <: ViewUpdateConfig

  def system = context.system

  implicit val ec: ExecutionContext = context.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer()

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
    val officeInfo = vuConfig.officeInfo
    handler.lastEventNumber.map { lastEvtNrOpt =>
      ViewUpdate(officeInfo, lastEvtNrOpt,
        eventSource(esCon, officeInfo, lastEvtNrOpt)
          .map {
            case ResolvedEvent(EventRecord(_, _, eventData, _), linkEvent) =>
              EventReceived(eventData, linkEvent.number.value, lastEvtNrOpt)
            case unexpected =>
              throw new RuntimeException(s"Unexpected msg received: $unexpected")
          }.filter {
            !_.alreadyProcessed
          }.mapAsync(1) {
            event => handler.handle(toDomainEventMessage(event.eventData).get, event.eventNr)
          }.toMat(Sink.ignore)(Keep.right)
      )
    }
  }

  def eventSource(esCon: EsConnection, oi: OfficeInfo[_], lastEvtNrOpt: Option[Long]): Source[Event, Unit] = { 
    val streamId = StreamNameResolver.streamId(OfficeEventStream(oi))
    Source.fromPublisher(
      esCon.streamPublisher(
        streamId,
        lastEvtNrOpt.map(nr => Exact(nr.toInt)),
        resolveLinkTos = true
      )
    )
  }

}
