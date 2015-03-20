package pl.newicom.dddd.view.sql

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend
import scala.slick.jdbc.meta.MTable

case class ViewMetadataRecord(id: Option[Long], viewId: String, lastEventNr: Long)

class ViewMetadataDao(implicit val profile: JdbcProfile) {
  import profile.simple._

  object ViewMetadata {
    val TableName = "view_metadata"
  }

  class ViewMetadata(tag: Tag) extends Table[ViewMetadataRecord](tag, ViewMetadata.TableName) {
    def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    def viewId = column[String]("VIEW_ID", O.NotNull)
    def lastEventNr = column[Long]("LAST_EVENT_NR", O.NotNull)
    def * = (id, viewId, lastEventNr) <> (ViewMetadataRecord.tupled, ViewMetadataRecord.unapply)
  }

  val viewMetadata = TableQuery[ViewMetadata]

  private val by_view_id = viewMetadata.findBy(_.viewId)

  def byViewId(viewId: String)(implicit s: Session) = by_view_id(viewId).run.headOption

  def insertOrUpdate(viewId: String, lastEventNr: Long)(implicit session: Session) {
    val query = by_view_id(viewId)
    val oldOpt = query.run.headOption
    if (oldOpt.isDefined) {
      viewMetadata.filter(_.viewId === viewId).map(v => v.lastEventNr).update(lastEventNr)
    } else {
      viewMetadata.insert(ViewMetadataRecord(None, viewId, lastEventNr))
    }
  }

  def lastEventNr(viewId: String)(implicit session: JdbcBackend.Session): Option[Long] = {
    byViewId(viewId).map(record => record.lastEventNr)
  }

  def dropSchema(implicit s: JdbcBackend.Session) =
    viewMetadata.ddl.drop

  def createSchema(implicit s: JdbcBackend.Session) = {
    if (MTable.getTables(ViewMetadata.TableName).list.isEmpty) {
      viewMetadata.ddl.create
    }
  }

}