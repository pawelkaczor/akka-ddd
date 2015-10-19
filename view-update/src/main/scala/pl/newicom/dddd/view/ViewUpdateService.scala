package pl.newicom.dddd.view

import akka.actor.Status.Failure
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.stream.ActorMaterializer
import eventstore.EsConnection
import pl.newicom.dddd.view.ViewUpdateInitializer.ViewUpdateInitException
import pl.newicom.dddd.view.ViewUpdateFactory.ViewUpdate
import pl.newicom.dddd.view.ViewUpdateService.{ViewUpdateConfigured, EnsureViewStoreAvailable, ViewUpdateInitiated}
import pl.newicom.eventstore.EventstoreSerializationSupport
import akka.pattern.pipe

import scala.concurrent.{ExecutionContext, Future}

object ViewUpdateService {
  case class ViewUpdateInitiated(esConnection: EsConnection)
  case class ViewUpdateConfigured(viewUpdates: Seq[ViewUpdate])
  object EnsureViewStoreAvailable
}

abstract class ViewUpdateService extends Actor with ActorLogging {

  type Configuration <: ViewUpdateConfig

  implicit val ec: ExecutionContext = context.dispatcher

  implicit val materializer = ActorMaterializer()

  def configurations: Seq[Configuration]

  def viewHandler(config: Configuration): ViewHandler

  def ensureViewStoreAvailable: Future[Unit]

  def onViewUpdateInitiated: Future[Unit] = {
    // override
    Future.successful(())
  }
  
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
    case ViewUpdateInitiated(esConn) =>
      val factory = new ViewUpdateFactory with EventstoreSerializationSupport {
        def esConnection = esConn
        def system       = context.system
      }
      onViewUpdateInitiated.flatMap(_ =>
        Future.sequence(
          configurations.map { config =>
            factory.runnableViewUpdate(config.officeInfo, viewHandler(config))
          }
        ).map { viewUpdates =>
          ViewUpdateConfigured(viewUpdates)
        }
      ) pipeTo self

    case ViewUpdateConfigured(viewUpdates) =>
      viewUpdates.foreach(_.runnable.run())

    case Failure(ex) =>
      throw ex

    case EnsureViewStoreAvailable =>
      ensureViewStoreAvailable pipeTo sender()

    case unexpected =>
      throw new RuntimeException(s"Unexpected message received: $unexpected")
  }

}
