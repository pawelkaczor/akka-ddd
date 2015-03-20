package pl.newicom.dddd.view.sql

import org.scalatest.{Suite, BeforeAndAfterAll}
import org.slf4j.LoggerFactory.getLogger

import scala.slick.driver.H2Driver
import scala.slick.jdbc.JdbcBackend

trait SqlViewStoreTestSupport extends SqlViewStoreConfiguration with BeforeAndAfterAll {
  this: Suite =>

  val log = getLogger(getClass)

  implicit val profile = H2Driver

  def dropSchema(implicit s: JdbcBackend.Session)
  def createSchema(implicit s: JdbcBackend.Session)

  override def beforeAll() {
    import scala.slick.jdbc.JdbcBackend._

    viewStore withSession { implicit session: Session =>
      try {
        dropSchema(session)
      } catch {
        case ex: Exception => // ignore
      }
      createSchema(session)
    }

  }

}