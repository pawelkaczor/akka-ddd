package pl.newicom.eventstore

import akka.actor.ActorSystem
import eventstore.{EventStoreExtension, EsConnection}

trait EventStoreConfiguration {
    def system: ActorSystem

    lazy val esExtension = EventStoreExtension(system)

    lazy val eventStore: EsConnection = esExtension.connection
}