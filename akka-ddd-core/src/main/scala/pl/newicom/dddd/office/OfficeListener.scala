package pl.newicom.dddd.office

import org.slf4j.LoggerFactory._

class OfficeListener[A : LocalOfficeId] {

    lazy val log = getLogger(getClass.getName)

    def officeStarted(office: OfficeRef): Unit = {
      log.info("{} Office of {} Department- started up successfully.", office.id, office.department:Any)
    }
}
