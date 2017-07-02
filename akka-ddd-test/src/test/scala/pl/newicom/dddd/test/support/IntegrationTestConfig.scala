package pl.newicom.dddd.test.support

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object IntegrationTestConfig {
  val config = ConfigFactory.parseString(
    """include "serialization"
      |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
      |akka.logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
      |akka.loglevel = DEBUG
      |akka.actor.debug.autoreceive = "on"
      |akka.persistence.journal.plugin = "eventstore.persistence.journal"
      |akka.persistence.snapshot-store.plugin = "eventstore.persistence.snapshot-store"
      |eventstore.persistence.journal {
      |  event-adapters {
      |    tagger = "pl.newicom.dddd.persistence.TaggingEventAdapter"
      |  }
      |
      |  event-adapter-bindings {
      |    "pl.newicom.dddd.messaging.event.EventMessage" = tagger
      |  }
      |}
      |app.view-store.config {
      |  driver = "slick.driver.H2Driver$"
      |  db {
      |    connectionPool = disabled
      |    driver = "org.h2.Driver"
      |    url = "jdbc:h2:mem:view_update_sql_test;DB_CLOSE_DELAY=-1"
      |  }
      |}
      """.stripMargin)

  def integrationTestSystem(name: String): ActorSystem = ActorSystem(name, config)

}
