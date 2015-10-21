package pl.newicom.dddd.view.sql

import eventstore.EsConnection
import pl.newicom.dddd.view.ViewUpdateService
import pl.newicom.dddd.view.ViewUpdateService.ViewUpdateInitiated
import slick.dbio.DBIO
import slick.dbio.DBIOAction.successful
import slick.driver.JdbcProfile

import scala.concurrent.Future

abstract class SqlViewUpdateService(implicit val profile: JdbcProfile) extends ViewUpdateService with FutureHelpers {
  this: SqlViewStoreConfiguration =>

  type VUConfig = SqlViewUpdateConfig

  override def ensureViewStoreAvailable: Future[Unit] = {
    viewStore.run(profile.defaultTables).mapToUnit
  }

  override def onViewUpdateInit(esCon: EsConnection): Future[ViewUpdateInitiated] =
    viewStore.run {
      onViewUpdateInit >> successful(ViewUpdateInitiated(esCon))
    }

  def onViewUpdateInit: DBIO[Unit] =
    successful(())


  override def viewHandler(vuConfig: VUConfig) =
    new SqlViewHandler(config, vuConfig)

}
