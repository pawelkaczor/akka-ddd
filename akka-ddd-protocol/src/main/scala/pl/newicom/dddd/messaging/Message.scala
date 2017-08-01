package pl.newicom.dddd.messaging

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.{Receipt, Processed, alod}
import pl.newicom.dddd.messaging.MetaData._

import scala.util.{Success, Try}

object PublisherType extends Enumeration {
  val AR, BP = Value
}

object MetaData {
  val DeliveryId    = "_deliveryId"
  val CausationId   = "causationId"
  val CorrelationId = "correlationId"
  // contains ID of a message that the recipient of this message should process before it can process this message
  val MustFollow    = "_mustFollow"
  val SessionId     = "sessionId"
  val EventNumber   = "_eventNumber"
  val Tags          = "tags"
  val PublisherType = "publisherType"
  val Reused        = "reused"

  def empty: MetaData = MetaData(Map.empty)
}

case class MetaData(content: Map[String, Any]) extends Serializable {

  def mergeWithMetadata(metadata: Option[MetaData]): MetaData = {
    metadata.map(_.content).map(add).getOrElse(this)
  }

  def add(content: Map[String, Any]): MetaData = {
    copy(content = this.content ++ content)
  }

  def remove(key: String): MetaData = {
    copy(content = this.content - key)
  }

  def contains(attrName: String): Boolean = content.contains(attrName)

  def get[B](attrName: String): B = tryGet[B](attrName).get

  def tryGet[B](attrName: String): Option[B] = content.get(attrName).asInstanceOf[Option[B]]

  def exceptDeliveryAttributes: Option[MetaData] = {
    val resultMap = this.content.filterKeys(a => !a.startsWith("_"))
    if (resultMap.isEmpty) None else Some(MetaData(resultMap))
  }

  override def toString: String = content.toString()
}

trait Message extends Serializable {

  def id: String

  type MessageImpl <: Message

  def causedBy(msg: Message): MessageImpl =
    withMetaData(msg.metadataExceptDeliveryAttributes)
      .withCausationId(msg.id)
      .asInstanceOf[MessageImpl]

  def metadataExceptDeliveryAttributes: Option[MetaData] = {
    metadata.flatMap(_.exceptDeliveryAttributes)
  }

  def withMetaData(metadata: Option[MetaData]): MessageImpl = {
    copyWithMetaData(this.metadata.map(_.mergeWithMetadata(metadata)).orElse(metadata))
  }

  def withMetaData(metadataContent: Map[String, Any]): MessageImpl = {
    withMetaData(Some(MetaData(metadataContent)))
  }

  def copyWithMetaData(m: Option[MetaData]): MessageImpl

  def metadata: Option[MetaData]

  def withoutMetaAttribute(attrName: String): MessageImpl =
    copyWithMetaData(metadata.map(_.remove(attrName)))

  def withMetaAttribute(attrName: String, value: Any): MessageImpl =
    withMetaData(Map(attrName -> value))

  def hasMetaAttribute(attrName: String): Boolean =
    metadata.exists(_.contains(attrName))

  def getMetaAttribute[B](attrName: String): B =
    tryGetMetaAttribute[B](attrName).get

  def tryGetMetaAttribute[B](attrName: String): Option[B] =
    if (metadata.isDefined) metadata.get.tryGet[B](attrName) else None

  def deliveryReceipt(result: Try[Any] = Success("OK")): Receipt = {
    deliveryId.map(id => alod.Processed(id, result)).getOrElse(Processed(result))
  }

  def withDeliveryId(deliveryId: Long): MessageImpl =
    withMetaAttribute(DeliveryId, deliveryId)

  def withEventNumber(eventNumber: Int): MessageImpl =
    withMetaAttribute(EventNumber, eventNumber)

  def withCorrelationId(correlationId: EntityId): MessageImpl =
    withMetaAttribute(CorrelationId, correlationId)

  def withCausationId(causationId: EntityId): MessageImpl =
    withMetaAttribute(CausationId, causationId)

  def withMustFollow(mustFollow: Option[String]): MessageImpl =
    mustFollow.map(msgId => withMetaAttribute(MustFollow, msgId)).getOrElse(this.asInstanceOf[MessageImpl])

  def withSessionId(sessionId: EntityId): MessageImpl =
    withMetaAttribute(SessionId, sessionId)

  def withTag(tag: String): MessageImpl =
    withMetaAttribute(MetaData.Tags, tags + tag)

  def withTags(tags: String*): MessageImpl =
    withMetaAttribute(MetaData.Tags, this.tags ++ tags)

  def withPublisherType(publisherType: PublisherType.Value): MessageImpl =
    withMetaAttribute(MetaData.PublisherType, publisherType.toString)

  def withReused(reused: Boolean): MessageImpl =
    if (reused)
      withMetaAttribute(Reused, reused)
    else
      this.asInstanceOf[MessageImpl]

  def tags: Set[String] =
    tryGetMetaAttribute[Set[String]](Tags).toSet.flatten

  def deliveryId: Option[Long] =
    tryGetMetaAttribute[Any](DeliveryId).map {
      case bigInt: scala.math.BigInt => bigInt.toLong
      case l: Long                   => l
    }

  def correlationId: Option[EntityId] =
    tryGetMetaAttribute[EntityId](CorrelationId)

  def causationId: Option[EntityId] =
    tryGetMetaAttribute[EntityId](CausationId)

  def mustFollow: Option[String] =
    tryGetMetaAttribute[String](MustFollow)

  def eventNumber: Option[Int] =
    tryGetMetaAttribute[Int](EventNumber)

  def publisherType: Option[PublisherType.Value] =
    tryGetMetaAttribute[String](MetaData.PublisherType).map(PublisherType.withName)

  def reused: Option[Boolean] =
    tryGetMetaAttribute[Boolean](Reused)

}
