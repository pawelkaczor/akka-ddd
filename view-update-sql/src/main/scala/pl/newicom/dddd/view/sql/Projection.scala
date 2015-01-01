package pl.newicom.dddd.view.sql

import pl.newicom.dddd.messaging.event.DomainEventMessage

import scala.slick.driver.JdbcProfile

trait Projection {

  def consume(event: DomainEventMessage)(implicit session: JdbcProfile#Backend#Session)

}
