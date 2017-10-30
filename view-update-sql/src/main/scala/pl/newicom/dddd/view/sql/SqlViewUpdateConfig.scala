package pl.newicom.dddd.view.sql

import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.view.ViewUpdateConfig

case class SqlViewUpdateConfig(
                                override val viewName: String,
                                override val eventSource: BusinessEntity,
                                projections: Projection*)
  extends ViewUpdateConfig