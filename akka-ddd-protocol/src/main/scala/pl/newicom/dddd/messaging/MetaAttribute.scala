package pl.newicom.dddd.messaging

import enumeratum.EnumEntry.LowerCamelcase
import enumeratum._
import org.joda.time.DateTime
import pl.newicom.dddd.utils.ImplicitUtils._

import scala.collection.immutable

sealed trait MetaAttribute[A] extends LowerCamelcase {

  def read(value: Any): A =
    value.asInstanceOf[A]
}

object MetaAttribute extends Enum[MetaAttribute[_]] {

  case object Causation_Id   extends MetaAttribute[String]

  case object Correlation_Id extends MetaAttribute[String]

  case object Delivery_Id    extends MetaAttribute[Long] {
    override def read(value: Any): Long = value match {
      case bigInt: scala.math.BigInt => bigInt.toLong
      case l: Long                   => l
    }
  }

  case object Event_Number   extends MetaAttribute[Long]

  case object Id             extends MetaAttribute[String]

  // contains ID of a message that the recipient of this message should process before it can process this message
  case object Must_Follow    extends MetaAttribute[String]

  case object Publisher_Type extends MetaAttribute[PublisherTypeValue.Value] {
    override def read(value: Any): PublisherTypeValue.Value =
      PublisherTypeValue.withName(value.toString)
  }

  case object Reused extends MetaAttribute[Boolean]

  case object Tags extends MetaAttribute[Set[String]] {
    def merge(md1: MetaData, md2: MetaData): Set[String] = {
      (md1.tryGet(Tags) ++ md2.tryGet(Tags)).flatten.toSet
    }
  }

  case object Target extends MetaAttribute[String]

  case object Timestamp extends MetaAttribute[DateTime] {
    override def read(value: Any): DateTime = DateTime.parse(value.toString)
  }

  def apply(name: String): Option[MetaAttribute[Any]] =
    withNameInsensitiveOption(name).asParameterizedBy[MetaAttribute[Any]]

  val values: immutable.IndexedSeq[MetaAttribute[_]] = findValues

}

object PublisherTypeValue extends Enumeration {
  val AR, BP = Value
}
