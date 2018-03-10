package pl.newicom.dddd.view.sql

import pl.newicom.dddd.Eventsourced
import pl.newicom.dddd.view.ViewUpdateConfig

case class SqlViewUpdateConfig(
                                override val viewName: String,
                                override val eventSource: Eventsourced,
                                projections: Projection*)
  extends ViewUpdateConfig