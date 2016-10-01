package pl.newicom.dddd.aggregate

import pl.newicom.dddd.office.OfficeListener

trait AggregateRootSupport {

  implicit def officeListener[A <: AggregateRoot[_, _]]: OfficeListener[A] = new OfficeListener[A]

}
