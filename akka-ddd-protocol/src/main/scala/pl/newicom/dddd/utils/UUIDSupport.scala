package pl.newicom.dddd.utils

import java.util.UUID

object UUIDSupport {
  def uuid: String               = uuidObj.toString.replaceAllLiterally("-", "")
  def uuid7: String              = uuid.substring(0, 6)
  def uuid10: String             = uuid.substring(0, 9)
  def uuidObj: UUID              = UUID.randomUUID()
  def uuid(uuid: String): String = UUID.nameUUIDFromBytes(uuid.getBytes()).toString
}

trait UUIDSupport {
  def uuid: String               = UUIDSupport.uuid
  def uuid7: String              = UUIDSupport.uuid7
  def uuid10: String             = UUIDSupport.uuid10
  def uuidObj: UUID              = UUIDSupport.uuidObj
  def uuid(uuid: String): String = UUIDSupport.uuid(uuid)
}
