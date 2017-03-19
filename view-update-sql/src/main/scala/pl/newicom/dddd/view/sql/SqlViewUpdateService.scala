package pl.newicom.dddd.view.sql

import pl.newicom.dddd.messaging.event.EventSourceProvider
import pl.newicom.dddd.view.ViewUpdateService
import pl.newicom.dddd.view.ViewUpdateService.{ViewUpdateFailed, ViewUpdateInitiated}
import slick.dbio.DBIO
import slick.dbio.DBIOAction.successful
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

abstract class SqlViewUpdateService(viewStore: SqlViewStore)(implicit val profile: JdbcProfile) extends ViewUpdateService with FutureHelpers {
  this: EventSourceProvider=>

  type VUConfig = SqlViewUpdateConfig

  override def ensureViewStoreAvailable: Future[Unit] =
    viewStore.run(profile.defaultTables).mapToUnit

  override def onViewUpdateInit: Future[ViewUpdateInitiated.type] =
    viewStore.run {
      viewUpdateInitAction >> successful(ViewUpdateInitiated)
    }

  override def onViewUpdateFailed(reason: Throwable): Future[ViewUpdateFailed] = {
    viewStore.close()
    super.onViewUpdateFailed(reason)
  }

  def viewUpdateInitAction: DBIO[Unit] =
    new ViewMetadataDao().ensureSchemaCreated


  override def viewHandler(vuConfig: VUConfig) =
    new SqlViewHandler(viewStore, vuConfig)

}
