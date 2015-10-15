package pl.newicom.dddd.view.sql

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory.getLogger
import slick.dbio.DBIO
import slick.driver.H2Driver

trait SqlViewStoreTestSupport extends SqlViewStoreConfiguration with BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  val log = getLogger(getClass)

  implicit val profile = H2Driver

  def ensureSchemaDropped: DBIO[Unit]
  def ensureSchemaCreated: DBIO[Unit]

  override def beforeAll() {
    val setup = viewStore.run {
      ensureSchemaDropped >> ensureSchemaCreated
    }
    assert(setup.isReadyWithin(Span(5, Seconds)))

  }

}