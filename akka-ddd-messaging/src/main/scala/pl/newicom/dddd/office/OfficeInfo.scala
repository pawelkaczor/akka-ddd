package pl.newicom.dddd.office

import pl.newicom.dddd.serialization.{JsonSerializationHints, NoSerializationHints}

import scala.reflect.ClassTag

object OfficeInfo {
  def apply[A](_serializationHints: JsonSerializationHints)(implicit ct: ClassTag[A]): OfficeInfo[A] =
    new OfficeInfo[A] {
      def serializationHints = _serializationHints
      def name = ct.runtimeClass.getSimpleName
    }

  def apply[A](_name: String, _serializationHints: JsonSerializationHints): OfficeInfo[A] =
    new OfficeInfo[A] {
      def serializationHints = _serializationHints
      def name = _name
    }

  def apply[A](_name: String): OfficeInfo[A] =
    new OfficeInfo[A] {
      def serializationHints = NoSerializationHints
      def name = _name
    }

}

trait OfficeInfo[A] {
  def name: String
  def isSagaOffice: Boolean = false
  def serializationHints: JsonSerializationHints
}