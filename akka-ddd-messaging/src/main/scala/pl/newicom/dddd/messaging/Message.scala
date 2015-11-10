package pl.newicom.dddd.messaging

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.{Receipt, Processed, alod}
import pl.newicom.dddd.messaging.MetaData._

import scala.util.{Success, Try}

object MetaData {
  val DeliveryId          = "_deliveryId"
  val CausationId         = "causationId"
  val CorrelationId       = "correlationId"
  val SessionId           = "sessionId"

  def empty: MetaData = MetaData(Map.empty)
}

case class MetaData(content: Map[String, Any]) extends Serializable {

  def mergeWithMetadata(metadata: Option[MetaData]): MetaData = {
    metadata.map(_.content).map(addContent).getOrElse(this)
  }

  def addContent(content: Map[String, Any]): MetaData = {
    copy(content = this.content ++ content)
  }

  def contains(attrName: String) = content.contains(attrName)

  def get[B](attrName: String) = tryGet[B](attrName).get

  def tryGet[B](attrName: String): Option[B] = content.get(attrName).asInstanceOf[Option[B]]

  def exceptDeliveryAttributes: Option[MetaData] = {
    val resultMap = this.content.filterKeys(a => !a.startsWith("_"))
    if (resultMap.isEmpty) None else Some(new MetaData(resultMap))
  }

  override def toString: String = content.toString()
}

trait Message extends Serializable {

  def id: String

  type MessageImpl <: Message

  def causedBy(msg: Message): MessageImpl =
    withMetaData(msg.metadataExceptDeliveryAttributes)
      .withCausationId(msg.id).asInstanceOf[MessageImpl]

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

  def withMetaAttribute(attrName: Any, value: Any): MessageImpl = withMetaData(Map(attrName.toString -> value))

  def hasMetaAttribute(attrName: Any) = if (metadata.isDefined) metadata.get.contains(attrName.toString) else false

  def getMetaAttribute[B](attrName: Any) = tryGetMetaAttribute[B](attrName).get

  def tryGetMetaAttribute[B](attrName: Any): Option[B] = if (metadata.isDefined) metadata.get.tryGet[B](attrName.toString) else None

  def deliveryReceipt(result: Try[Any] = Success("OK")): Receipt = {
    deliveryId.map(id => alod.Processed(id, result)).getOrElse(Processed(result))
  }

  def withDeliveryId(deliveryId: Long) = withMetaAttribute(DeliveryId, deliveryId)

  def withCorrelationId(correlationId: EntityId) = withMetaAttribute(CorrelationId, correlationId)

  def withCausationId(causationId: EntityId) = withMetaAttribute(CausationId, causationId)

  def withSessionId(sessionId: EntityId) = withMetaAttribute(SessionId, sessionId)

  def deliveryId: Option[Long] = tryGetMetaAttribute[Any](DeliveryId).map {
    case bigInt: scala.math.BigInt => bigInt.toLong
    case l: Long => l
  }

  def correlationId: Option[EntityId] = tryGetMetaAttribute[EntityId](CorrelationId)

}