package pl.newicom.dddd.view.sql

import com.typesafe.config.Config
import org.scalactic.Equality
import org.scalatest._
import pl.newicom.dddd.test.support.TestConfig

import scala.concurrent.ExecutionContext.Implicits.global

class ViewMetadataDaoSpec extends WordSpecLike with Matchers with SqlViewStoreTestSupport {

  def config: Config = TestConfig.config

  implicit val _ = new Equality[ViewMetadataRecord] {
    def areEqual(a: ViewMetadataRecord, b: Any): Boolean =
      b match {
        case b_rec: ViewMetadataRecord => a.copy(id = -1) == b_rec.copy(id = -1)
        case _ => false
      }
  }

  val dao = new ViewMetadataDao()

  "ViewMetadataDao" should {
    "insert new entry if view does not exist" in {
      // When
      dao.insertOrUpdate("test view", 0).run()

      // Then
      dao.byViewId("test view").result should not be 'empty
    }
  }

  "ViewMetadataDao" should {
    "insert & update entry" in {
      // When
      dao.insertOrUpdate("test view", 0).run()
      dao.insertOrUpdate("test view", 1).run()

      // Then
      dao.byViewId("test view").result.get should equal (ViewMetadataRecord(1, "test view", 1))
    }
  }

  override def ensureSchemaDropped = dao.ensureSchemaDropped


  override def ensureSchemaCreated = dao.ensureSchemaCreated

}
