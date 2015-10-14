package pl.newicom.dddd.view.sql

import pl.newicom.dddd.view.ViewUpdateService

import scala.concurrent.{ExecutionContext, Future}
import slick.driver.JdbcProfile
import FutureHelpers._

abstract class SqlViewUpdateService(implicit val profile: JdbcProfile) extends ViewUpdateService {
  this: SqlViewStoreConfiguration =>

  type Configuration = SqlViewUpdateConfig

  override def ensureViewStoreAvailable(implicit ec: ExecutionContext): Future[Unit] = {
    viewStore.run(profile.defaultTables).mapToUnit
  }

  override def viewHandler(vuConfig: Configuration)(implicit ec: ExecutionContext) = new SqlViewHandler(config, vuConfig)

}
