package pl.newicom.dddd.view.sql

import com.typesafe.config.{ConfigFactory, Config}
import org.scalactic.Equality
import org.scalatest._
import scala.concurrent.ExecutionContext.Implicits.global

class ViewMetadataDaoSpec extends WordSpecLike with Matchers with SqlViewStoreTestSupport {

  def config: Config = ConfigFactory.load()

  implicit val _ = new Equality[ViewMetadataRecord] {
    def areEqual(a: ViewMetadataRecord, b: Any): Boolean =
      b match {
        case b_rec: ViewMetadataRecord => a.copy(id = Some(-1)) == b_rec.copy(id = Some(-1))
        case _ => false
      }
  }

  val dao = new ViewMetadataDao()

  "ViewMetadataDao" should {
    "insert new entry if view does not exist" in {
      // When
      viewStore run {
        dao.insertOrUpdate("test view", 0)
      }

      // Then
      viewStore run {
        dao.byViewId("test view") /*should not be 'empty*/
      }
    }
  }

  "ViewMetadataDao" should {
    "insert & update entry" in {
      // When
      viewStore.run {
        dao.insertOrUpdate("test view", 0)
        dao.insertOrUpdate("test view", 1)
      }

      // Then
      viewStore.run {
        dao.byViewId("test view")/*.get should equal (ViewMetadataRecord(Some(1), "test view", 1))*/
      }
    }
  }

  override def ensureSchemaDropped = dao.ensureSchemaDropped


  override def ensureSchemaCreated = dao.ensureSchemaCreated

}
