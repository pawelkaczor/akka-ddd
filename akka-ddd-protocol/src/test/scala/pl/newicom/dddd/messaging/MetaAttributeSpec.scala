package pl.newicom.dddd.messaging

import pl.newicom.dddd.messaging.MetaAttribute.{Id, Tags}

class MetaAttributeSpec extends org.scalatest.FunSuite {

  test("merge empty meta data") {
    val event = MetaData.empty.withAttr(Id, "e-1")
    val command = MetaData.empty.withAttr(Id, "c-1")
    val md = MetaDataPropagationPolicy.onCommandSentByPM(event, command)
    assert(md != null)
  }

  test("merge meta data") {
    val event = MetaData.empty.withAttr(Id, "e-1").withAttr(Tags, Set("t1, t2"))
    val command = MetaData.empty.withAttr(Id, "c-1").withAttr(Tags, Set("t3", "t4"))
    val md = MetaDataPropagationPolicy.onCommandSentByPM(event, command)
    assert(md != null)
  }

  test("merge meta data 2") {
    val event = MetaData.empty.withAttr(Id, "e-1").withAttr(Tags, Set.empty[String])
    val command = MetaData.empty.withAttr(Id, "c-1").withAttr(Tags, Set("t3", "t4"))
    val md = MetaDataPropagationPolicy.onCommandSentByPM(event, command)
    assert(md != null)
  }

  test("merge meta data 3") {
    val event = MetaData.empty.withAttr(Id, "e-1").withAttr(Tags, Set("t3", "t4"))
    val command = MetaData.empty.withAttr(Id, "c-1").withAttr(Tags, Set.empty[String])
    val md = MetaDataPropagationPolicy.onCommandSentByPM(event, command)
    assert(md != null)
  }

  test("merge meta data 4") {
    val event = MetaData.empty.withAttr(Id, "e-1").withAttr(Tags, Set.empty[String])
    val command = MetaData.empty.withAttr(Id, "c-1").withAttr(Tags, Set.empty[String])
    val md = MetaDataPropagationPolicy.onCommandSentByPM(event, command)
    assert(md != null)
  }

  test("merge meta data 5") {
    val event = MetaData.empty.withAttr(Id, "e-1")
    val command = MetaData.empty.withAttr(Id, "c-1").withAttr(Tags, Set("t3", "t4"))
    val md = MetaDataPropagationPolicy.onCommandSentByPM(event, command)
    assert(md != null)
  }

  test("merge meta data 6") {
    val event = MetaData.empty.withAttr(Id, "e-1").withAttr(Tags, Set("t3", "t4"))
    val command = MetaData.empty.withAttr(Id, "c-1")
    val md = MetaDataPropagationPolicy.onCommandSentByPM(event, command)
    assert(md != null)
  }


}
