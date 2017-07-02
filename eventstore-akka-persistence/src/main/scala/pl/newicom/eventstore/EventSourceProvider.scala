package pl.newicom.eventstore

import akka.actor.Actor
import akka.persistence.eventstore.query.scaladsl.EventStoreReadJournal
import pl.newicom.dddd.messaging.event.{EventSourceProvider => ESP}

trait EventSourceProvider extends ESP with EventStoreProvider {
  this: Actor =>

  val readJournalId = EventStoreReadJournal.Identifier

}
