package pl.newicom.dddd.test

import pl.newicom.dddd.office.OfficeInfo

package object dummy {

  implicit val dummyOffice: OfficeInfo[DummyAggregateRoot] = OfficeInfo[DummyAggregateRoot]()

}
