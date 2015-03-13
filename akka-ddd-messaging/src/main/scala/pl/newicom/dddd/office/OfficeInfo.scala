package pl.newicom.dddd.office

import pl.newicom.dddd.serialization.JsonSerializationHints

trait OfficeInfo[A] {
  def name: String
  def streamName: String = name
  def serializationHints: JsonSerializationHints
}