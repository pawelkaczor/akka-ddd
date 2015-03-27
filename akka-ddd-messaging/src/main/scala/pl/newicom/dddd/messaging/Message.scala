package pl.newicom.dddd.messaging

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.{Receipt, Processed, alod}
import pl.newicom.dddd.messaging.MetaData.{CorrelationId, DeliveryId}

import scala.util.{Success, Try}

object MetaData {
  val DeliveryId = "_deliveryId"
  val CorrelationId: String = "correlationId"
  val SessionId: String = "sessionId"
}

class MetaData(var metadata: Map[String, Any] = Map.empty) extends Serializable {

  def withMetaData(metadata: Option[MetaData]): MetaData = {
    if (metadata.isDefined) withMetaData(metadata.get.metadata) else this
  }

  def withMetaData(metadata: Map[String, Any], clearExisting: Boolean = false): MetaData = {
    if (clearExisting) {
      this.metadata = Map.empty
    }
    new MetaData(this.metadata ++ metadata)
  }

  def contains(attrName: String) = metadata.contains(attrName)

  def get[B](attrName: String) = tryGet[B](attrName).get

  def tryGet[B](attrName: String): Option[B] = metadata.get(attrName).asInstanceOf[Option[B]]

  def exceptDeliveryAttributes: Option[MetaData] = {
    val resultMap = this.metadata.filterKeys(a => !a.startsWith("_"))
    if (resultMap.isEmpty) None else Some(new MetaData(resultMap))
  }

  override def toString: String = metadata.toString()
}

abstract class Message(var metadata: Option[MetaData] = None) extends Serializable {

  def id: String

  type MessageImpl <: Message

  def metadataExceptDeliveryAttributes: Option[MetaData] = {
    metadata.flatMap(_.exceptDeliveryAttributes)
  }

  def withMetaData(metadata: Option[MetaData]): MessageImpl = {
    if (metadata.isDefined) withMetaData(metadata.get.metadata) else this.asInstanceOf[MessageImpl]
  }

  def withMetaData(metadata: Map[String, Any], clearExisting: Boolean = false): MessageImpl = {
    this.metadata = Some(this.metadata.getOrElse(new MetaData()).withMetaData(metadata))
    this.asInstanceOf[MessageImpl]
  }

  def withMetaAttribute(attrName: Any, value: Any): MessageImpl = withMetaData(Map(attrName.toString -> value))

  def hasMetaAttribute(attrName: Any) = if (metadata.isDefined) metadata.get.contains(attrName.toString) else false

  def getMetaAttribute[B](attrName: Any) = tryGetMetaAttribute[B](attrName).get

  def tryGetMetaAttribute[B](attrName: Any): Option[B] = if (metadata.isDefined) metadata.get.tryGet[B](attrName.toString) else None

  def deliveryReceipt(result: Try[Any] = Success("OK")): Receipt = {
    if (deliveryId.isDefined) alod.Processed(deliveryId.get, result) else Processed(result)
  }

  def withDeliveryId(deliveryId: Long) = withMetaAttribute(DeliveryId, deliveryId)

  def withCorrelationId(correlationId: EntityId) = withMetaAttribute(CorrelationId, correlationId)

  def deliveryId: Option[Long] = tryGetMetaAttribute[Any](DeliveryId).map {
    case bigInt: scala.math.BigInt => bigInt.toLong
    case l: Long => l
  }

  def correlationId: Option[EntityId] = tryGetMetaAttribute[EntityId](CorrelationId)

}