package pl.newicom.dddd.serialization

import org.json4s.{Formats, Serializer, TypeHints}

trait JsonSerializationHints {

  def typeHints: TypeHints
  def serializers: List[Serializer[_]]

  def ++ (formats: Formats): Formats = formats ++ serializers + typeHints

  def ++ (other: JsonSerializationHints): JsonSerializationHints = new Tuple2(typeHints, serializers) with JsonSerializationHints {
    override def typeHints: TypeHints = _1 + other.typeHints
    override def serializers: List[Serializer[_]] = _2 ++ other.serializers
  }
}
