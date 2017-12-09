package pl.newicom.dddd.test

import pl.newicom.dddd.office.LocalOfficeId
import pl.newicom.dddd.test.dummy.DummySaga.DummySagaConfig

package object dummy {

  implicit val dummyOfficeId: LocalOfficeId[DummyAggregateRoot] =
    new LocalOfficeId[DummyAggregateRoot](classOf[DummyAggregateRoot].getSimpleName, "dummy")

  implicit val testSagaConfig: DummySagaConfig =
    new DummySagaConfig("DummySaga")

}
