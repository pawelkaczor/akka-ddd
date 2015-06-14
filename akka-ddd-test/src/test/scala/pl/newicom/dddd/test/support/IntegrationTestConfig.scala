package pl.newicom.dddd.test.support

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object IntegrationTestConfig {
  val config = ConfigFactory.parseString(
    """include "serialization"
      |akka.loggers = ["akka.testkit.TestEventListener"]
      |akka.loglevel = DEBUG
      |akka.actor.debug.autoreceive = "on"
      |akka.persistence.journal.plugin = "eventstore.persistence.journal"
      |akka.persistence.snapshot-store.plugin = "eventstore.persistence.snapshot-store"
    """.stripMargin)

  def integrationTestSystem(name: String): ActorSystem = ActorSystem(name, config)

}
