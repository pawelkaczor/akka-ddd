package pl.newicom.dddd.process

import akka.actor.{ActorRef, ActorSystem}
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, CreationSupport}
import pl.newicom.dddd.aggregate.DomainEvent
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.office.{Office, OfficeFactory}

object SagaSupport {
  type ExchangeName = String

  type ExchangeSubscriptions[A <: Saga] = Map[ExchangeName, Array[Class[_ <: DomainEvent]]]

  implicit def defaultCaseIdResolution[A <: Saga]() = new EntityIdResolution[A]

  def registerSaga[A <: Saga : ExchangeSubscriptions : EntityIdResolution : OfficeFactory : BusinessEntityActorFactory](implicit system: ActorSystem, creator: CreationSupport): ActorRef = {
    val sagaOffice = Office.office[A]
    registerEventListeners(sagaOffice)
    sagaOffice
  }

  private def registerEventListeners[A <: Saga](sagaOffice: ActorRef)(implicit es: ExchangeSubscriptions[_], creator: CreationSupport) {
    for ((exchangeName, events) <- es) {
      // TODO implement
      //ForwardingConsumer(exchangeName, sagaOffice)
    }
  }
}
