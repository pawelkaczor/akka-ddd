package pl.newicom.dddd.view.sql

import pl.newicom.dddd.office.OfficeInfo
import pl.newicom.dddd.view.ViewUpdateConfig

case class SqlViewUpdateConfig(
    override val viewName: String,
    override val officeInfo: OfficeInfo[_],
    projections: Projection*)
  extends ViewUpdateConfig