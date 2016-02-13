package pl.newicom.dddd.test

import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.test.dummy.DummySaga.DummySagaConfig

package object dummy {

  implicit val dummyOfficeId = new LocalOfficeId[DummyAggregateRoot](classOf[DummyAggregateRoot].getSimpleName, "dummy")

  implicit val testSagaConfig = new DummySagaConfig("DummySaga")

}
