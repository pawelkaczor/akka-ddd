package pl.newicom.dddd.utils

import java.util.UUID

object UUIDSupport {
  def uuid: String = uuidObj.toString.replaceAllLiterally("-", "")
  def uuid7: String = uuid.substring(0, 6)
  def uuidObj: UUID = UUID.randomUUID()
}

trait UUIDSupport {
  def uuid = UUIDSupport.uuid
  def uuid7 = UUIDSupport.uuid7
  def uuidObj = UUIDSupport.uuidObj
}
