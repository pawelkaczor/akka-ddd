package pl.newicom.dddd.view.sql

import pl.newicom.dddd.view.ViewUpdateConfig

case class SqlViewUpdateConfig(
    override val viewName: String,
    override val streamName: String,
    projections: Projection*)
  extends ViewUpdateConfig