package pl.newicom.dddd.view.sql

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.meta.MTable

case class ViewMetadataRecord(id: Long, viewId: String, lastEventNr: Long)

class ViewMetadataDao(implicit val profile: JdbcProfile) {
  import profile.simple._

  object ViewMetadata {
    val TableName = "view_metadata"
  }

  class ViewMetadata(tag: Tag) extends Table[ViewMetadataRecord](tag, ViewMetadata.TableName) {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def viewId = column[String]("VIEW_ID", O.NotNull)
    def lastEventNr = column[Long]("LAST_EVENT_NR", O.NotNull)
    def * = (id, viewId, lastEventNr) <> (ViewMetadataRecord.tupled, ViewMetadataRecord.unapply)
  }

  val viewMetadata = TableQuery[ViewMetadata]

  private val by_view_id = viewMetadata.findBy(_.viewId)

  def byViewId(viewId: String)(implicit s: Session) = by_view_id(viewId).run.headOption

  def create(implicit s: Session) =
    if (MTable.getTables(ViewMetadata.TableName).list.isEmpty) {
      viewMetadata.ddl.create
    }

  def drop(implicit s: Session) = viewMetadata.ddl.drop

  def insertOrUpdate(viewId: String, lastEventNr: Long)(implicit session: Session) {
    val query = by_view_id(viewId)
    val oldOpt = query.run.headOption
    if (oldOpt.isDefined) {
      query.update(oldOpt.get.copy(lastEventNr = lastEventNr))
    } else {
      viewMetadata.insert(ViewMetadataRecord(-1, viewId, lastEventNr))
    }
  }

  def lastEventNr(viewId: String)(implicit session: Session): Option[Long] = {
    byViewId(viewId).map(record => record.lastEventNr)
  }

  def dropSchema(implicit s: Session) =
    viewMetadata.ddl.drop

  def createSchema(implicit s: Session) = {
    if (MTable.getTables(ViewMetadata.TableName).list.isEmpty) {
      viewMetadata.ddl.create
    }
  }

}