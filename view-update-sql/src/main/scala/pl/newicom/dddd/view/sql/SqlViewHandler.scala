package pl.newicom.dddd.view.sql

import com.typesafe.config.Config
import pl.newicom.dddd.messaging.event.DomainEventMessage
import pl.newicom.dddd.view.ViewHandler

import scala.slick.driver.JdbcProfile

class SqlViewHandler(override val config: Config, override val vuConfig: SqlViewUpdateConfig)
                    (implicit val profile: JdbcProfile)
  extends ViewHandler(vuConfig) with SqlViewStoreConfiguration {

  private lazy val viewMetadataDao = new ViewMetadataDao

  def handle(eventMessage: DomainEventMessage, eventNumber: Long) =
     viewStore withTransaction  { implicit s =>
       vuConfig.projections.foreach(_.consume(eventMessage))
       viewMetadataDao.insertOrUpdate(viewName, eventNumber)
     }

  def lastEventNumber: Option[Long] = {
    viewStore withSession { implicit session =>
      viewMetadataDao.lastEventNr(viewName)
    }
  }
}
