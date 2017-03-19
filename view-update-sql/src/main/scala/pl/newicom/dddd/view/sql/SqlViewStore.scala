package pl.newicom.dddd.view.sql

import com.typesafe.config.Config
import slick.basic.DatabaseConfig
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.JdbcProfile
import slick.jdbc.JdbcBackend

import scala.concurrent.Future

class SqlViewStore(config: Config) {

  lazy val db: JdbcBackend#DatabaseDef = {
    DatabaseConfig.forConfig[JdbcProfile]("app.view-store.config", config).db
  }

  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] =
    db.run(a)

  def close(): Unit =
    db.close()

}