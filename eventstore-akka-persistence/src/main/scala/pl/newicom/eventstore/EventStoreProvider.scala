package pl.newicom.eventstore

import akka.actor.ActorSystem
import eventstore.EsConnection
import eventstore.EventStream.System

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait EventStoreProvider extends pl.newicom.dddd.messaging.event.EventStoreProvider {
    def system: ActorSystem

    type EventStore = EsConnection

    override def eventStore: EsConnection = EsConnection(system)

    override def ensureEventStoreAvailable: Future[EsConnection] = {
        val esConn = eventStore
        esConn.getStreamMetadata(System("test")).map(_ => esConn)
    }

}