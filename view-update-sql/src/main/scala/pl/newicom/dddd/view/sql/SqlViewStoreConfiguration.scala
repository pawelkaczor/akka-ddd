package pl.newicom.dddd.view.sql

import com.typesafe.config.Config

import slick.jdbc.JdbcBackend._

trait SqlViewStoreConfiguration {
  def config: Config

  val dbUrl = config.getString("app.view.store.url")
  val dbDriver = config.getString("app.view.store.driver")

  lazy val viewStore: Database = {
    Database.forURL(url = dbUrl, driver = dbDriver)
  }

}