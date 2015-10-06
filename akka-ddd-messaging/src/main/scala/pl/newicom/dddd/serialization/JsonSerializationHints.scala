package pl.newicom.dddd.serialization

import org.json4s.{NoTypeHints, Formats, Serializer, TypeHints}

trait JsonSerializationHints {

  def typeHints: TypeHints
  def serializers: List[Serializer[_]]

  def ++ (formats: Formats): Formats = formats ++ serializers + typeHints

  def ++ (other: JsonSerializationHints): JsonSerializationHints = (typeHints, serializers) match {
    case (myTypeHints, mySerializers) => new JsonSerializationHints {
      override def typeHints: TypeHints = myTypeHints + other.typeHints
      override def serializers: List[Serializer[_]] = mySerializers ++ other.serializers
    }
  }
}

object NoSerializationHints extends JsonSerializationHints {
  def typeHints = NoTypeHints
  def serializers = List()
}