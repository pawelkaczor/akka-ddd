package pl.newicom.dddd.view.sql

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.meta.MTable

class ViewMetadataDao(implicit val profile: JdbcProfile) {
  import profile.simple._

  case class ViewMetadataRecord(id: String, viewId: String, lastEventNr: Int)

  object ViewMetadata {
    val TableName = "view_metadata"
  }

  class ViewMetadata(tag: Tag) extends Table[ViewMetadataRecord](tag, ViewMetadata.TableName) {
    def id = column[String]("ID", O.PrimaryKey, O.AutoInc)
    def viewId = column[String]("VIEW_ID", O.NotNull)
    def lastEventNr = column[Int]("LAST_EVENT_NR", O.NotNull)
    def * = (id, viewId, lastEventNr) <> (ViewMetadataRecord.tupled, ViewMetadataRecord.unapply)
  }

  val viewMetadata = TableQuery[ViewMetadata]

  def create(implicit s: Session) =
    if (MTable.getTables(ViewMetadata.TableName).list.isEmpty) {
      viewMetadata.ddl.create
    }

  def drop(implicit s: Session) = viewMetadata.ddl.drop

  def insertOrUpdate(viewId: String, lastEventNr: Int)(implicit session: Session) {
    viewMetadata.insertOrUpdate(ViewMetadataRecord(null, viewId, lastEventNr))
  }

  def lastEventNr(viewId: String)(implicit session: Session): Option[Int] = {
    viewMetadata.filter(_.viewId === viewId).firstOption.map(record => record.lastEventNr)
  }
}