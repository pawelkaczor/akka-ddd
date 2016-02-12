package pl.newicom.dddd.view.sql

import pl.newicom.dddd.messaging.event.EventSourceProvider
import pl.newicom.dddd.view.ViewUpdateService
import pl.newicom.dddd.view.ViewUpdateService.ViewUpdateInitiated
import slick.dbio.DBIO
import slick.dbio.DBIOAction.successful
import slick.driver.JdbcProfile

import scala.concurrent.Future

abstract class SqlViewUpdateService[ES](implicit val profile: JdbcProfile) extends ViewUpdateService[ES] with FutureHelpers {
  this: SqlViewStoreConfiguration with EventSourceProvider[ES] =>

  type VUConfig = SqlViewUpdateConfig

  override def ensureViewStoreAvailable: Future[Unit] = {
    viewStore.run(profile.defaultTables).mapToUnit
  }

  override def onViewUpdateInit(eventStore: ES): Future[ViewUpdateInitiated[ES]] =
    viewStore.run {
      onViewUpdateInit >> successful(ViewUpdateInitiated(eventStore))
    }

  def onViewUpdateInit: DBIO[Unit] =
    new ViewMetadataDao().ensureSchemaCreated


  override def viewHandler(vuConfig: VUConfig) =
    new SqlViewHandler(config, vuConfig)

}
