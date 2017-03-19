package pl.newicom.dddd.view.sql

import com.typesafe.config.Config
import org.scalatest._
import pl.newicom.dddd.test.support.TestConfig

import scala.concurrent.ExecutionContext.Implicits.global

class ViewMetadataDaoSpec extends WordSpecLike with Matchers with SqlViewStoreTestSupport {

  def config: Config = TestConfig.config

  override val viewStore = new SqlViewStore(config)

  val dao = new ViewMetadataDao()

  val id = ViewMetadataId("test view", "test stream")

  "ViewMetadataDao" should {
    "insert new entry if view does not exist" in {
      // When
      dao.insertOrUpdate(id, 0).run()

      // Then
      dao.byId(id).result should not be 'empty
    }
  }

  "ViewMetadataDao" should {
    "insert & update entry" in {
      // When
      dao.insertOrUpdate(id, 0).run()
      dao.insertOrUpdate(id, 1).run()

      // Then
      dao.byId(id).result.get should equal (ViewMetadataRecord("test view", "test stream", 1))
    }
  }

  override def ensureSchemaDropped = dao.ensureSchemaDropped


  override def ensureSchemaCreated = dao.ensureSchemaCreated

}
