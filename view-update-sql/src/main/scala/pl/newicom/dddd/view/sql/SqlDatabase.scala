package pl.newicom.dddd.view.sql

import org.joda.time.{DateTime, DateTimeZone}
import slick.driver.JdbcProfile

case class SqlDatabase(
                        db: slick.jdbc.JdbcBackend.Database,
                        driver: JdbcProfile,
                        connectionString: JdbcConnectionString
                        ) {

  import driver.api._

  implicit val dateTimeColumnType = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new java.sql.Timestamp(dt.getMillis),
    t => new DateTime(t.getTime).withZone(DateTimeZone.UTC)
  )

  def close() {
    db.close()
  }
}

case class JdbcConnectionString(url: String, username: String = "", password: String = "")

