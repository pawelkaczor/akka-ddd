package pl.newicom.dddd.view.sql

import pl.newicom.dddd.messaging.event.DomainEventMessage

import scala.slick.jdbc.JdbcBackend

trait Projection {

  def consume(event: DomainEventMessage)(implicit session: JdbcBackend.Session)

}
