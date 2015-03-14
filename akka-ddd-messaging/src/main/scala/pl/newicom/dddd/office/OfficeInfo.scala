package pl.newicom.dddd.office

import pl.newicom.dddd.serialization.JsonSerializationHints

object OfficeInfo {
  def apply[A](_name: String, _serializationHints: JsonSerializationHints): OfficeInfo[A] =
    new OfficeInfo[A] {
      def serializationHints = _serializationHints
      def name = _name
    }
}

trait OfficeInfo[A] {
  def name: String
  def streamName: String = name
  def serializationHints: JsonSerializationHints
}