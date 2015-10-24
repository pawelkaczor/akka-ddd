package pl.newicom.dddd.persistence

import akka.persistence.PersistentActor

trait ForgettingParticularEvents {
  this: PersistentActor =>

  override def journalPluginId = "akka.persistence.journal.inmem"
}
