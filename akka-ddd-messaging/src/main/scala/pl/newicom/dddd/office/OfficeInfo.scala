package pl.newicom.dddd.office

import scala.reflect.ClassTag

object OfficeInfo {
  def apply[A]()(implicit ct: ClassTag[A]): OfficeInfo[A] =
    new OfficeInfo[A] {
      def name = ct.runtimeClass.getSimpleName
    }

}

trait OfficeInfo[A] {
  def name: String
  def isSagaOffice: Boolean = false
}