package pl.newicom.dddd.process

import akka.actor.ActorPath
import pl.newicom.dddd.messaging.{Message, MetaData}
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}
import scala.concurrent.duration._

class SagaManager(sagaConfig: SagaConfig[_], sagaOffice: ActorPath) extends Receptor(sagaConfig, sagaOffice) {
  this: EventStreamSubscriber =>

  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  override def outgoingMessage: PartialFunction[EventMessage, Message] = {
    case em => em
  }

  override def metaDataProvider(em: EventMessage): Option[MetaData] =
    sagaConfig.correlationIdResolver.lift(em.event).map { correlationId =>
      new MetaData(Map(CorrelationId -> correlationId))
    }

}
