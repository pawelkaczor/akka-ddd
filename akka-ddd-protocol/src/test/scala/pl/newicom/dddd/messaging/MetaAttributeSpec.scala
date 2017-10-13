package pl.newicom.dddd.messaging

import pl.newicom.dddd.messaging.MetaAttribute.Tags
import pl.newicom.dddd.messaging.MetaData.empty

class MetaAttributeSpec extends org.scalatest.FunSuite {

  test("merge tags (both are empty)") {
    val tags = Tags.merge(empty.withAttr(Tags, Set.empty[String]), empty.withAttr(Tags, Set.empty[String]))
    assert(tags == Set())
  }

  test("merge tags (both are missing)") {
    val tags = Tags.merge(empty, empty)
    assert(tags.isEmpty)
  }

  test("merge tags (first is missing)") {
    val tags = Tags.merge(empty.withAttr(Tags, Set("t1", "t2")), empty)
    assert(tags == Set("t1", "t2"))
  }

  test("merge tags (second is missing)") {
    val tags = Tags.merge(empty, empty.withAttr(Tags, Set("t1", "t2")))
    assert(tags == Set("t1", "t2"))
  }

  test("merge tags (both are not empty)") {
    val tags = Tags.merge(empty.withAttr(Tags, Set("t3", "t4")), empty.withAttr(Tags, Set("t1", "t2")))
    assert(tags == Set("t1", "t2", "t3", "t4"))
  }

}
