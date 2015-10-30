package pl.newicom.dddd.test.support

import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.ActorSystem

object TestConfig {
  val config = ConfigFactory.parseString(
    """akka.loggers = ["akka.testkit.TestEventListener"]
      |akka.loglevel = DEBUG
      |akka.log-config-on-start = "off"
      |akka.actor.debug.autoreceive = "on"
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |app.view-store.config {
      |  driver = "slick.driver.H2Driver$"
      |  db {
      |    driver = "org.h2.Driver"
      |    url = "jdbc:h2:mem:view_update_sql_test;DB_CLOSE_DELAY=-1"
      |  }
      |}
    """.stripMargin)

  implicit def testSystem: ActorSystem = testSystem(TestConfig.config)

  def testSystem(config: Config) = {
    ActorSystem("Tests", config)
  }

}
