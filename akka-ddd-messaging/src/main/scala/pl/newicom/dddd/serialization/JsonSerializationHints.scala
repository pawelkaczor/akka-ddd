package pl.newicom.dddd.serialization

import org.json4s.{Formats, Serializer, TypeHints}

trait JsonSerializationHints[A] {

  def typeHints: TypeHints
  def serializers: List[Serializer[_]]

  def ++ (formats: Formats): Formats = formats ++ serializers + typeHints
}
