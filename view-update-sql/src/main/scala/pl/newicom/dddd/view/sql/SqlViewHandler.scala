package pl.newicom.dddd.view.sql

import akka.Done
import pl.newicom.dddd.messaging.event.OfficeEventMessage
import pl.newicom.dddd.view.ViewHandler
import slick.dbio.DBIOAction.sequence
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class SqlViewHandler(val viewStore: SqlViewStore, override val vuConfig: SqlViewUpdateConfig)
                    (implicit val profile: JdbcProfile, ex: ExecutionContext)
  extends ViewHandler(vuConfig) with FutureHelpers {

  import profile.api._

  private lazy val viewMetadataDao = new ViewMetadataDao

  def viewMetadataId = ViewMetadataId(viewName, vuConfig.eventSource.id)

  def handle(eventMessage: OfficeEventMessage, eventNumber: Long): Future[Done] =
    viewStore.run {
      (sequence(vuConfig.projections.map(_.consume(eventMessage))) >>
        viewMetadataDao.insertOrUpdate(viewMetadataId, eventNumber)).transactionally
    }.mapToDone

  def lastEventNumber: Future[Option[Long]] =
    viewStore.run {
      viewMetadataDao.lastEventNr(viewMetadataId)
    }

}
