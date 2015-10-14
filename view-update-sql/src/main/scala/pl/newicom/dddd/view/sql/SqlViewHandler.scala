package pl.newicom.dddd.view.sql

import com.typesafe.config.Config
import pl.newicom.dddd.messaging.event.DomainEventMessage
import pl.newicom.dddd.view.ViewHandler
import slick.dbio.{NoStream, DBIOAction}
import slick.dbio.DBIOAction.sequence
import slick.driver.JdbcProfile
import FutureHelpers._
import scala.concurrent.{ExecutionContext, Future}

class SqlViewHandler(override val config: Config, override val vuConfig: SqlViewUpdateConfig)
                    (implicit val profile: JdbcProfile, ex: ExecutionContext)
  extends ViewHandler(vuConfig) with SqlViewStoreConfiguration {

  private lazy val viewMetadataDao = new ViewMetadataDao

  def handle(eventMessage: DomainEventMessage, eventNumber: Long): Future[Unit] = run {
    sequence(vuConfig.projections.map(_.consume(eventMessage))) >>
    viewMetadataDao.insertOrUpdate(viewName, eventNumber)
  }.mapToUnit

  def lastEventNumber: Future[Option[Long]] = run {
    viewMetadataDao.lastEventNr(viewName)
  }

  private def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = viewStore.run(a)

}
