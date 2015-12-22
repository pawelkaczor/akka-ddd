package pl.newicom.dddd.view.sql

import pl.newicom.dddd.messaging.event.OfficeEventMessage
import pl.newicom.dddd.view.sql.Projection.ProjectionAction
import slick.dbio.Effect.All
import slick.dbio.{DBIOAction, Effect, NoStream}

object Projection {
  type ProjectionAction[E <: Effect] = DBIOAction[Unit, NoStream, E]
}

trait Projection extends DBActionHelpers {

  def consume(event: OfficeEventMessage): ProjectionAction[All]

}
