package pl.newicom.dddd.process

import akka.actor.ActorPath
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.MetaData._
import pl.newicom.dddd.messaging.event.{EventMessage, EventStreamSubscriber}

class SagaManager(sagaConfig: SagaConfig[_], sagaOffice: ActorPath) extends Receptor(sagaConfig, sagaOffice) {
  this: EventStreamSubscriber =>

  override def metaDataProvider(em: EventMessage): Option[MetaData] =
    sagaConfig.correlationIdResolver.lift(em.event).map { correlationId =>
      new MetaData(Map(CorrelationId -> correlationId))
    }

}
