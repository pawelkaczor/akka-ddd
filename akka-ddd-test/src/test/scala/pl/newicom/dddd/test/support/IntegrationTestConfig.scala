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
      |app.view.store.url = "jdbc:h2:mem:view_update_sql_test;DB_CLOSE_DELAY=-1"
      |app.view.store.driver = "org.h2.Driver"
    """.stripMargin)

  def integrationTestSystem(name: String): ActorSystem = ActorSystem(name, config)

}
