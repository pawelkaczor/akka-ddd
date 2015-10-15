package pl.newicom.dddd.view.sql

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory.getLogger
import slick.dbio.DBIO
import slick.driver.H2Driver

import scala.concurrent.ExecutionContext

trait SqlViewStoreTestSupport extends SqlViewStoreConfiguration with BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  val log = getLogger(getClass)

  implicit val profile = H2Driver

  implicit class ViewStoreAction[A](a: DBIO[A])(implicit ex: ExecutionContext) {
    private val future = viewStore.run(a)

    def run(): Unit = future.map(_ => ()).futureValue
    def result: A = future.futureValue
  }

  def ensureSchemaDropped: DBIO[Unit]
  def ensureSchemaCreated: DBIO[Unit]

  override def beforeAll() {
    val setup = viewStore.run {
      ensureSchemaDropped >> ensureSchemaCreated
    }
    assert(setup.isReadyWithin(Span(5, Seconds)))

  }

}