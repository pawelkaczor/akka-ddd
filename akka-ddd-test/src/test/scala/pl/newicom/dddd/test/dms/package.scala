package pl.newicom.dddd.test

import pl.newicom.dddd.office.LocalOfficeId

package object dms {

  implicit val documentOfficeId: LocalOfficeId[DocumentAR] =
    new LocalOfficeId[DocumentAR](classOf[DocumentAR].getSimpleName, "dms")

}
