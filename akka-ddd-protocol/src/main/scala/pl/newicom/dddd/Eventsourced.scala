package pl.newicom.dddd

trait Eventsourced {
  this: BusinessEntity =>

  def streamName: String = id

  def department: String
}
