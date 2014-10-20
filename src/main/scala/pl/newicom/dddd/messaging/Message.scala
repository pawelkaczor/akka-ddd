package pl.newicom.dddd.messaging

import pl.newicom.dddd.messaging.Message.MetaData

object Message {
  type MetaData = scala.collection.mutable.Map[Any, Any]
}

abstract class Message(metaData: Option[MetaData]) extends Serializable {

  def withMetaData(metaData: Map[Any, Any], clearExisting: Boolean = false): Message = {
    if (clearExisting) {
      this.metaData.foreach(_.clear())
    }
    this.metaData.getOrElse(scala.collection.mutable.Map.empty).++=(metaData)
    this
  }

  def withMetaAttribute(attrName: Any, value: Any): Message = withMetaData(Map(attrName -> value))

  def hasMetaAttribute(attrName: Any) = metaData.flatMap(_.get(attrName)).isDefined

  def getMetaAttribute[B](attrName: Any) = tryGetMetaAttribute[B](attrName).get

  def tryGetMetaAttribute[B](attrName: Any): Option[B] = metaData.flatMap(_.get(attrName)).asInstanceOf[Option[B]]
}