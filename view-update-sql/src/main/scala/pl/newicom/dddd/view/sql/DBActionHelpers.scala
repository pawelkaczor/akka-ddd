package pl.newicom.dddd.view.sql

import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.ExecutionContext

trait DBActionHelpers {

  implicit def mapToUnit[R, E <: Effect](action: DBIOAction[R, NoStream, E])(implicit ec: ExecutionContext): DBIOAction[Unit, NoStream, E] =
    action.map(_ => ())

}