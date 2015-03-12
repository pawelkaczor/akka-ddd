package pl.newicom.dddd.serialization

import org.json4s.{Formats, Serializer, TypeHints}

trait JsonSerializationHints[A] {

  def typeHints: TypeHints
  def serializers: List[Serializer[_]]

  def ++ (formats: Formats): Formats = formats ++ serializers + typeHints

  def ++ (other: JsonSerializationHints[_]): JsonSerializationHints[_] = new Tuple2(typeHints, serializers) with JsonSerializationHints[AnyRef] {
    override def typeHints: TypeHints = _1 + other.typeHints
    override def serializers: List[Serializer[_]] = _2 ++ other.serializers
  }
}
