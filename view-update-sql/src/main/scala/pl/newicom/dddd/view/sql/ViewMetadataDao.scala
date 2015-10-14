package pl.newicom.dddd.view.sql

import slick.dbio.DBIOAction.successful
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables

import scala.concurrent.ExecutionContext

case class ViewMetadataRecord(id: Option[Long], viewId: String, lastEventNr: Long)

class ViewMetadataDao(implicit val profile: JdbcProfile, ex: ExecutionContext) extends SqlViewMetadataSchema {

  import profile.api._

  private val by_view_id = viewMetadata.findBy(_.viewId)

  def byViewId(viewId: String) = {
    by_view_id(viewId).result
  }

  def insertOrUpdate(viewId: String, lastEventNr: Long) =
    by_view_id(viewId).result.filter(_.isEmpty).cleanUp {
      case Some(viewNotFoundError) =>
        viewMetadata.forceInsert(ViewMetadataRecord(None, viewId, lastEventNr))
      case None =>
        viewMetadata.filter(_.viewId === viewId).map(v => v.lastEventNr).update(lastEventNr)
    }


  def lastEventNr(viewId: String) =
    by_view_id(viewId).result.headOption.map(_.map(_.lastEventNr))


  def dropSchema =
    viewMetadata.schema.drop

  def createSchema() =
    getTables(viewMetadataTableName)
      .filter(_.isEmpty)
      .cleanUp({
        case Some(tableNotFoundError) => viewMetadata.schema.create
        case None => successful(())
      }, keepFailure = false).map(_ => ())
}

trait SqlViewMetadataSchema {

  protected val profile: JdbcProfile

  import profile.api._

  protected val viewMetadata = TableQuery[ViewMetadata]

  protected val viewMetadataTableName = "view_metadata"

  protected class ViewMetadata(tag: Tag) extends Table[ViewMetadataRecord](tag, viewMetadataTableName) {
    def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    def viewId = column[String]("VIEW_ID")
    def lastEventNr = column[Long]("LAST_EVENT_NR")
    def * = (id, viewId, lastEventNr) <> (ViewMetadataRecord.tupled, ViewMetadataRecord.unapply)
  }


}