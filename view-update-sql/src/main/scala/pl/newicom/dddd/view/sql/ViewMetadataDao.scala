package pl.newicom.dddd.view.sql

import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables

import scala.concurrent.ExecutionContext

case class ViewMetadataRecord(id: Long, viewId: String, lastEventNr: Long)

class ViewMetadataDao(implicit val profile: JdbcProfile, ex: ExecutionContext) extends SqlViewMetadataSchema {

  import profile.api._

  private val by_view_id = viewMetadata.findBy(_.viewId)

  def byViewId(viewId: String) = {
    by_view_id(viewId).result.headOption
  }

  def insertOrUpdate(viewId: String, lastEventNr: Long) =
    byViewId(viewId).flatMap {
      case None =>
        viewMetadata.forceInsert(ViewMetadataRecord(0, viewId, lastEventNr))
      case Some(view) =>
        viewMetadata.filter(_.viewId === viewId).map(v => v.lastEventNr).update(lastEventNr)
    }


  def lastEventNr(viewId: String) =
    by_view_id(viewId).result.headOption.map(_.map(_.lastEventNr))


  def ensureSchemaDropped =
    getTables(viewMetadataTableName).headOption.flatMap {
      case Some(table) => viewMetadata.schema.drop.map(_ => ())
      case None => DBIO.successful(())
    }

  def ensureSchemaCreated =
    getTables(viewMetadataTableName).headOption.flatMap {
        case Some(table) => DBIO.successful(())
        case None => viewMetadata.schema.create.map(_ => ())
    }
}

trait SqlViewMetadataSchema {

  protected val profile: JdbcProfile

  import profile.api._

  protected val viewMetadata = TableQuery[ViewMetadata]

  protected val viewMetadataTableName = "view_metadata"

  protected class ViewMetadata(tag: Tag) extends Table[ViewMetadataRecord](tag, viewMetadataTableName) {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def viewId = column[String]("VIEW_ID")
    def lastEventNr = column[Long]("LAST_EVENT_NR")
    def * = (id, viewId, lastEventNr) <> (ViewMetadataRecord.tupled, ViewMetadataRecord.unapply)
  }


}