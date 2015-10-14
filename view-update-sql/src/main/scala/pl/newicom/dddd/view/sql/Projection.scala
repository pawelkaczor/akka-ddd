package pl.newicom.dddd.view.sql

import pl.newicom.dddd.messaging.event.DomainEventMessage
import pl.newicom.dddd.view.sql.Projection.ProjectionAction
import slick.dbio.DBIO

object Projection {
  type ProjectionAction = DBIO[Unit]
}

trait Projection {

  def consume(event: DomainEventMessage): ProjectionAction

}
