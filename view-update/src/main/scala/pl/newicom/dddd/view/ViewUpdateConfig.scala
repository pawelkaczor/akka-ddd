package pl.newicom.dddd.view

import pl.newicom.dddd.Eventsourced

abstract class ViewUpdateConfig {
  def viewName: String
  def eventSource: Eventsourced
}