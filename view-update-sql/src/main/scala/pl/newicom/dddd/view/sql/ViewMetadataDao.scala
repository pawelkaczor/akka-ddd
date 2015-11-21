package pl.newicom.dddd.view.sql

import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables

import scala.concurrent.ExecutionContext

case class ViewMetadataRecord(viewId: String, streamId: String, lastEventNr: Long)
case class ViewMetadataId(viewId: String, streamId: String)

class ViewMetadataDao(implicit val profile: JdbcProfile, ex: ExecutionContext) extends SqlViewMetadataSchema {

  import profile.api._

  private def by_pk(viewId: Rep[String], streamId: Rep[String]) = for {
    result <- viewMetadata if result.viewId === viewId && result.streamId === streamId
  } yield {
    result
  }
  private val byPk = Compiled(by_pk _)

  def byId(id: ViewMetadataId) = {
    byPk(id.viewId, id.streamId).result.headOption
  }

  def insertOrUpdate(id: ViewMetadataId, lastEventNr: Long) =
    byId(id).flatMap {
      case None =>
        viewMetadata.forceInsert(ViewMetadataRecord(id.viewId, id.streamId, lastEventNr))
      case Some(view) =>
        viewMetadata.filter(r => r.viewId === id.viewId && r.streamId === id.streamId).map(v => v.lastEventNr).update(lastEventNr)
    }


  def lastEventNr(id: ViewMetadataId) =
    byPk(id.viewId, id.streamId).result.headOption.map(_.map(_.lastEventNr))


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
    def viewId = column[String]("VIEW_ID")
    def streamId = column[String]("STREAM_ID")
    def lastEventNr = column[Long]("LAST_EVENT_NR")
    def pk = primaryKey("pk_view_metadata", (viewId, streamId))
    def * = (viewId, streamId, lastEventNr) <> (ViewMetadataRecord.tupled, ViewMetadataRecord.unapply)
  }


}