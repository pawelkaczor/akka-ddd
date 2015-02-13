package pl.newicom.dddd.messaging

object MetaData {
  val DeliveryId = "deliveryId"
  val EventPosition = "eventPosition"
  val CorrelationId: String = "correlationId"
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

  override def toString: String = metadata.toString()
}

abstract class Message(var metadata: Option[MetaData] = None) extends Serializable {

  def id: String

  type MessageImpl <: Message

  def withMetaData(metadata: Option[MetaData]): MessageImpl = {
    if (metadata.isDefined) withMetaData(metadata.get.metadata) else this.asInstanceOf[MessageImpl]
  }

  def withMetaData(metadata: Map[String, Any], clearExisting: Boolean = false): MessageImpl = {
    this.metadata = Some(this.metadata.getOrElse(new MetaData()).withMetaData(metadata))
    this.asInstanceOf[MessageImpl]
  }

  def withMetaData2(metadata: Map[String, Any], clearExisting: Boolean = false): MessageImpl = {
    this.metadata = Some(this.metadata.getOrElse(new MetaData()).withMetaData(metadata))
    this.asInstanceOf[MessageImpl]
  }

  def withMetaAttribute(attrName: Any, value: Any): MessageImpl = withMetaData(Map(attrName.toString -> value))

  def hasMetaAttribute(attrName: Any) = if (metadata.isDefined) metadata.get.contains(attrName.toString) else false

  def getMetaAttribute[B](attrName: Any) = tryGetMetaAttribute[B](attrName).get

  def tryGetMetaAttribute[B](attrName: Any): Option[B] = if (metadata.isDefined) metadata.get.tryGet[B](attrName.toString) else None

}