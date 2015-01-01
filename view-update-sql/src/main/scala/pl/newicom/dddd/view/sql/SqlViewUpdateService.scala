package pl.newicom.dddd.view.sql

import pl.newicom.dddd.view.ViewUpdateService

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.JdbcProfile

abstract class SqlViewUpdateService(implicit val profile: JdbcProfile) extends ViewUpdateService {
  this: SqlViewStoreConfiguration =>

  type Configuration = SqlViewUpdateConfig

  override def ensureViewStoreAvailable(implicit ec: ExecutionContext): Future[Unit] = {
    Future(viewStore.withSession(s => s.capabilities.supportsBatchUpdates))
  }

  override def viewHandler(vuConfig: Configuration) = new SqlViewHandler(config, vuConfig)

}
