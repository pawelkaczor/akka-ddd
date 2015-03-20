package pl.newicom.dddd.test

import org.json4s.FullTypeHints
import pl.newicom.dddd.office.OfficeInfo
import pl.newicom.dddd.serialization.JsonSerializationHints
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._

package object dummy {

  implicit val dummyOffice = OfficeInfo[DummyAggregateRoot](new JsonSerializationHints {
    def typeHints = FullTypeHints(List(
      classOf[DummyCreated],
      classOf[NameChanged],
      classOf[DescriptionChanged],
      classOf[ValueChanged],
      classOf[ValueGenerated]
    ))
    def serializers = List()
  })

}
