package pl.newicom.dddd.process

import akka.actor.{ActorPath, Timers}
import akka.contrib.pattern.ReceivePipeline
import akka.persistence.{DeleteMessagesSuccess, PersistentActor}
import pl.newicom.dddd.coordination.ReceptorConfig
import pl.newicom.dddd.delivery.AtLeastOnceDeliverySupport
import pl.newicom.dddd.messaging.Message
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.{DemandCallback, DemandConfig}
import pl.newicom.dddd.messaging.event._
import pl.newicom.dddd.persistence.{RegularSnapshotting, RegularSnapshottingConfig}
import pl.newicom.dddd.process.Receptor.{Cleanup, CleanupKey}

import scala.concurrent.duration._


trait ReceptorPersistence extends ReceivePipeline with RegularSnapshotting {
  this: PersistentActor =>

  override def journalPluginId = "akka.persistence.journal.inmem"
}

object Receptor {
  private case object CleanupKey
  case object Cleanup
}

abstract class Receptor(config: ReceptorConfig) extends AtLeastOnceDeliverySupport with ReceptorPersistence with Timers {
  this: EventStreamSubscriber =>

  override def redeliverInterval: FiniteDuration = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  val snapshottingConfig = RegularSnapshottingConfig(
    interest = receiveEvent,
    interval = config.capacity) // TODO: snapshoting interval should be configured independently of the receptor capacity ?

  def deadLetters: ActorPath = context.system.deadLetters.path

  def destination(msg: Message): ActorPath =
    config.receiverResolver.applyOrElse(msg, (_: Message) => {
      log.warning("No destination provided")
      deadLetters
    })

  override lazy val persistenceId: String =
    self.path.name

  var demandCallback: Option[DemandCallback] = None

  override def recoveryCompleted(): Unit = {
    demandCallback = Some(subscribe(
        observable   = config.stimuliSource,
        fromPosExcl  = lastSentDeliveryId,
        demandConfig = DemandConfig(
                         subscriberCapacity = config.capacity,
                         initialDemand = config.capacity - unconfirmedNumber)))

    timers.startPeriodicTimer(CleanupKey, Cleanup, config.cleanupInterval)

    log.info(s"Receptor $persistenceId subscribed to '${config.stimuliSource.streamName}' event stream " +
      s"from position: ${lastSentDeliveryId.getOrElse(0)}. " +
      s"Receptor's capacity: ${config.capacity}")
  }


  override def receiveCommand: Receive =
    receiveEvent.orElse(deliveryStateReceive).orElse {
      case Cleanup =>
        oldestUnconfirmedDeliveryId.map(_ - 1).orElse(lastSentDeliveryId)
          .foreach(deleteMessages)
      case DeleteMessagesSuccess(_) =>
        // do nothing
      case other =>
        log.warning(s"RECEIVED: $other")
    }

  def receiveEvent: Receive = {
    case EventMessageEntry(em, position, _) =>
      config.transduction.lift(em) match {
        case Some(msg) =>
          deliver(msg, deliveryId = position)
        case None =>
          demandCallback.foreach(_.onEventProcessed())
      }
  }

  override def deliveryConfirmed(deliveryId: Long): Unit =
    demandCallback.foreach(_.onEventProcessed())

}
