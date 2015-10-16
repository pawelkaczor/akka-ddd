package pl.newicom.dddd.view

import akka.actor.SupervisorStrategy._
import akka.actor._
import pl.newicom.dddd.view.ViewUpdateInitializer.ViewUpdatingInitializationException
import pl.newicom.dddd.view.ViewUpdateService.{EnsureViewStoreAvailable, Start}

import scala.concurrent.{ExecutionContext, Future}

object ViewUpdateService {
  case class Start(eventStoreConnection: ActorRef)
  object EnsureViewStoreAvailable
}

abstract class ViewUpdateService extends Actor with ActorLogging {

  type Configuration <: ViewUpdateConfig

  implicit val ec: ExecutionContext = context.dispatcher

  def configuration: Seq[Configuration]

  def viewHandler(config: Configuration): ViewHandler

  def ensureViewStoreAvailable: Future[Unit]

  def onUpdateStart(): Unit = {
    // override
  }
  
  /**
   * Restart ViewUpdateInitializer until it successfully obtains connection to ES or target database
   * During normal processing escalate all exceptions so that feeding is restarted
   */
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException               => Stop
    case _: ActorInitializationException       => Stop
    case _: ViewUpdatingInitializationException => Restart
    case _                                     => Escalate
  }


  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.actorOf(Props(new ViewUpdateInitializer(self)))
  }

  override def receive: Receive = {
    case Start(esConn) =>
      onUpdateStart()
      configuration.foreach {
        config => context.actorOf(ViewUpdater.props(esConn, config.officeInfo, viewHandler(config)))
      }
    case EnsureViewStoreAvailable =>
      import akka.pattern.pipe
      ensureViewStoreAvailable pipeTo sender()
    case unexpected =>
      throw new RuntimeException(s"Unexpected message received: $unexpected")
  }

}
