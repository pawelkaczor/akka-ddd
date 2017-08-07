package pl.newicom.dddd.messaging

import org.joda.time.DateTime.now
import pl.newicom.dddd.messaging.MetaAttribute.{Id, Timestamp}
import pl.newicom.dddd.utils.UUIDSupport.uuid

object MetaData {
  def empty: MetaData =
    new MetaData(Map.empty)

  def initial: MetaData =
    MetaData(Id -> uuid, Timestamp -> now)

  def apply(attrs: (MetaAttribute[_], Any)*): MetaData =
    new MetaData(attrs.toMap.map(kv => kv._1.entryName -> kv._2))
}

case class MetaData(content: Map[String, Any]) extends Serializable {

  def withMetaData(metadata: MetaData): MetaData =
    copy(content = this.content ++ metadata.content)

  def withAttr[A](key: MetaAttribute[A], value: A): MetaData =
    copy(content = this.content + (key.entryName -> value))

  def withOptionalAttr[A](key: MetaAttribute[A], value: Option[A]): MetaData =
    value.map(withAttr(key, _)).getOrElse(this)

  def remove(attr: MetaAttribute[_]): MetaData =
    remove(attr.entryName)

  def remove(key: String): MetaData =
    copy(content = this.content - key)

  def contains(attrName: String): Boolean =
    content.contains(attrName)

  def get[B](attrName: String): B =
    tryGet[B](attrName).get

  def get[B](attr: MetaAttribute[B]): B =
    tryGet(attr).get

  def tryGet[B](attrName: String): Option[B] =
    content.get(attrName).asInstanceOf[Option[B]]

  def tryGet[B](attr: MetaAttribute[B]): Option[B] =
    content.get(attr.entryName).map(attr.value)

  override def toString: String =
    content.toString
}
