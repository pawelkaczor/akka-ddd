package pl.newicom.dddd.view

import pl.newicom.dddd.BusinessEntity

abstract class ViewUpdateConfig {
  def viewName: String
  def eventSource: BusinessEntity
}