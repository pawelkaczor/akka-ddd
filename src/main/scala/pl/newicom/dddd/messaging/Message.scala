package pl.newicom.dddd.messaging

class MetaData(var metadata: Map[Any, Any] = Map.empty) {

  def withMetaData(metadata: Option[MetaData]): MetaData = {
    if (metadata.isDefined) withMetaData(metadata.get.metadata) else this
  }

  def withMetaData(metadata: Map[Any, Any], clearExisting: Boolean = false): MetaData = {
    if (clearExisting) {
      this.metadata = Map.empty
    }
    new MetaData(this.metadata ++ metadata)
  }

  def contains(attrName: Any) = metadata.contains(attrName)

  def get[B](attrName: Any) = tryGet[B](attrName).get

  def tryGet[B](attrName: Any): Option[B] = metadata.get(attrName).asInstanceOf[Option[B]]

  override def toString: String = metadata.toString()
}

abstract class Message(var metadata: Option[MetaData] = None) extends Serializable {

  def withMetaData[T <: Message](metadata: Option[MetaData]): T = {
    if (metadata.isDefined) withMetaData(metadata.get.metadata) else this.asInstanceOf[T]
  }

  def withMetaData[T <: Message](metadata: Map[Any, Any], clearExisting: Boolean = false): T = {
    this.metadata = Some(this.metadata.getOrElse(new MetaData()).withMetaData(metadata))
    this.asInstanceOf[T]
  }

  def withMetaAttribute[T <: Message](attrName: Any, value: Any): T = withMetaData(Map(attrName -> value))

  def hasMetaAttribute(attrName: Any) = if (metadata.isDefined) metadata.get.contains(attrName) else false

  def getMetaAttribute[B](attrName: Any) = tryGetMetaAttribute[B](attrName).get

  def tryGetMetaAttribute[B](attrName: Any): Option[B] = if (metadata.isDefined) metadata.get.tryGet[B](attrName) else None
}