package pl.newicom.dddd.test

import pl.newicom.dddd.office.LocalOfficeId

package object dms {

  implicit val documentOfficeId = new LocalOfficeId[DocumentAR](classOf[DocumentAR].getSimpleName, "dms")

}
