package pl.newicom.dddd.messaging

import enumeratum.EnumEntry.LowerCamelcase
import enumeratum._
import org.joda.time.DateTime

import scala.collection.immutable

sealed trait MetaAttribute[A] extends LowerCamelcase {
  def value(value: Any): A = value.asInstanceOf[A]
}

object MetaAttribute extends Enum[MetaAttribute[_]] {

  case object Causation_Id   extends MetaAttribute[String]

  case object Correlation_Id extends MetaAttribute[String]

  case object Delivery_Id    extends MetaAttribute[Long] {
    override def value(value: Any): Long = value match {
      case bigInt: scala.math.BigInt => bigInt.toLong
      case l: Long                   => l
    }
  }

  case object Event_Number   extends MetaAttribute[Int]

  case object Id             extends MetaAttribute[String]

  // contains ID of a message that the recipient of this message should process before it can process this message
  case object Must_Follow    extends MetaAttribute[String]

  case object Publisher_Type extends MetaAttribute[PublisherTypeValue.Value] {
    override def value(value: Any): PublisherTypeValue.Value =
      PublisherTypeValue.withName(value.toString)

  }

  case object Reused extends MetaAttribute[Boolean]

  case object Tags extends MetaAttribute[Set[String]] {
    def merge(md1: MetaData, md2: MetaData): Set[String] = {
      md1.tryGet(Tags).orElse(Some(Set())).flatMap(t1 => md2.tryGet(Tags).map(t1 ++ _)).getOrElse(Set())
    }
  }

  case object Target extends MetaAttribute[String]

  case object Timestamp extends MetaAttribute[DateTime] {
    override def value(value: Any): DateTime = DateTime.parse(value.toString)
  }

  def apply(name: String): Option[MetaAttribute[Any]] =
    withNameInsensitiveOption(name).asInstanceOf[Option[MetaAttribute[Any]]]

  val values: immutable.IndexedSeq[MetaAttribute[_]] = findValues

}

object PublisherTypeValue extends Enumeration {
  val AR, BP = Value
}
