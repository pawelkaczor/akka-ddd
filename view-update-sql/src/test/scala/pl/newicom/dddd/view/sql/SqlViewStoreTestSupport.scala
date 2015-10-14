package pl.newicom.dddd.view.sql

import org.scalatest.{Suite, BeforeAndAfterAll}
import org.slf4j.LoggerFactory.getLogger
import slick.dbio.DBIO

import slick.driver.H2Driver
import scala.concurrent.ExecutionContext.Implicits.global

trait SqlViewStoreTestSupport extends SqlViewStoreConfiguration with BeforeAndAfterAll {
  this: Suite =>

  val log = getLogger(getClass)

  implicit val profile = H2Driver

  def dropSchema: DBIO[Unit]
  def createSchema: DBIO[Unit]

  override def beforeAll() {

    viewStore.run {
      dropSchema.cleanUp ({
        case _ => createSchema
      }, keepFailure = false)

    }

  }

}