package pl.newicom.dddd.view.sql

import com.typesafe.config.Config
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.jdbc.JdbcBackend

trait SqlViewStoreConfiguration {
  def config: Config

  lazy val viewStore: JdbcBackend#DatabaseDef = {
    DatabaseConfig.forConfig[JdbcProfile]("app.view-store.config", config).db
  }

}