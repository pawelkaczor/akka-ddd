package pl.newicom.dddd.view.sql

import com.typesafe.config.{ConfigFactory, Config}
import org.scalactic.Equality
import org.scalatest._

import scala.slick.jdbc.JdbcBackend

class ViewMetadataDaoSpec extends WordSpecLike with Matchers with SqlViewStoreTestSupport {

  def config: Config = ConfigFactory.load()

  implicit val _ = new Equality[ViewMetadataRecord] {
    def areEqual(a: ViewMetadataRecord, b: Any): Boolean =
      b match {
        case b_rec: ViewMetadataRecord => a.copy(id = -1) == b_rec.copy(id = -1)
        case _ => false
      }
  }

  val dao = new ViewMetadataDao()

  import dao.profile.simple._

  "ViewMetadataDao" should {
    "insert new entry if view does not exist" in {
      // When
      viewStore withSession { implicit s: Session =>
        dao.insertOrUpdate("test view", 0)
      }

      // Then
      viewStore withSession { implicit s: Session =>
        dao.byViewId("test view") should not be 'empty
      }
    }
  }

  "ViewMetadataDao" should {
    "insert & update entry" in {
      // When
      viewStore withSession { implicit s: Session =>
        dao.insertOrUpdate("test view", 0)
        dao.insertOrUpdate("test view", 1)
      }

      // Then
      viewStore withSession { implicit s: Session =>
        dao.byViewId("test view").get should equal (ViewMetadataRecord(1, "test view", 1))
      }
    }
  }

  override def dropSchema(session: JdbcBackend.Session): Unit =
    dao.dropSchema(session)


  override def createSchema(session: JdbcBackend.Session): Unit =
    dao.createSchema(session)

}
