package pl.newicom.dddd.serialization

import com.typesafe.config.Config
import org.json4s._
import org.json4s.ext.{EnumNameSerializer, JodaTimeSerializers, UUIDSerializer}
import pl.newicom.dddd.messaging.PublisherTypeValue
import pl.newicom.dddd.serialization.JsonSerHints.DefaultSerializationHints
import pl.newicom.dddd.serialization.{JsonAbstractSerHints => AbstractHints, JsonExtraSerHints => ExtraHints, JsonSerHints => FinalizedHints}

sealed trait JsonAbstractSerHints {
  def ++ (other: AbstractHints): AbstractHints = this match {
    case extra: ExtraHints =>
      extra ++ other
    case fin: FinalizedHints => other match {
      case extra: ExtraHints =>
        extra ++ fin
      case _ =>
        throw new UnsupportedOperationException("Merging two instances of finalized hints not supported!")
    }
  }
}

case class JsonSerHints(extraHints: ExtraHints, formats: Formats = DefaultSerializationHints) extends AbstractHints {
  def toFormats: Formats =  formats ++ extraHints.serializers + extraHints.typeHints

  def ++ (other: ExtraHints): FinalizedHints = copy(
    extraHints = ExtraHints(
      extraHints.typeHints + other.typeHints,
      extraHints.serializers ++ other.serializers
    )
  )
}

case class JsonExtraSerHints(typeHints: TypeHints, serializers: List[Serializer[_]]) extends AbstractHints {

  override def ++ (other: AbstractHints): AbstractHints = other match {
    case extra: ExtraHints     => this ++ extra
    case fin:   FinalizedHints => this ++ fin
  }

  def ++ (other: ExtraHints): ExtraHints = (typeHints, serializers) match {
    case (myTypeHints, mySerializers) =>
      ExtraHints(
        typeHints = myTypeHints + other.typeHints,
        serializers = mySerializers ++ other.serializers
      )
  }

  def ++ (other: FinalizedHints): FinalizedHints = 
    other ++ this

}

object JsonSerHints {

  val NoExtraHints = ExtraHints(NoTypeHints, List())
  val DefaultSerializationHints = FinalizedHints(NoExtraHints, DefaultFormats ++ JodaTimeSerializers.all + UUIDSerializer + new EnumNameSerializer(PublisherTypeValue))

  def fromConfig(config: Config) = new FromConfigJsonSerializationHintsProvider(config).hints()

  def apply(formats: Formats): FinalizedHints = DefaultSerializationHints.copy(formats = formats)

  implicit def fromListOfClassNames(hints: List[String]): ExtraHints =
    ExtraHints(
      typeHints = if (hints.isEmpty) NoTypeHints else FullTypeHints(hints.map(Class.forName)),
      serializers = List()
    )

  implicit def toFormats(hints: AbstractHints): Formats = hints match {
    case extra: ExtraHints => (extra ++ DefaultSerializationHints).toFormats
    case fin: FinalizedHints => fin.toFormats
  }

}
