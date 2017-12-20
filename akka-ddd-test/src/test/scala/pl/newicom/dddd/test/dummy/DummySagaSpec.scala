package pl.newicom.dddd.test.dummy

import pl.newicom.dddd.aggregate.AggregateId
import pl.newicom.dddd.test.dummy.DummyProtocol._
import pl.newicom.dddd.test.pm.PMSpec
import pl.newicom.dddd.test.support.TestConfig.testSystem


class DummySagaSpec extends PMSpec[DummySaga](Some(testSystem)) {

  def dummyId: DummyId = AggregateId(pmId)

  "Dummy Saga" should {

    "react to ValueChanged event" in {
      given {
        DummyCreated(dummyId, "dummy name", "dummy description", 100)
      }
      .when {
        ValueChanged(dummyId, 101, 1L)
      }
      .expect { e =>
        ValueChanged(e.id, e.value, e.dummyVersion)
      }
    }

  }

}
