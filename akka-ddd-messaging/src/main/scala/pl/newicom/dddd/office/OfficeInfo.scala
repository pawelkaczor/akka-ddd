package pl.newicom.dddd.office

import scala.reflect.ClassTag

object OfficeInfo {
  def apply[A]()(implicit ct: ClassTag[A]): OfficeInfo[A] =
    new OfficeInfo[A] {
      def name = ct.runtimeClass.getSimpleName
    }

  def apply[A](_name: String): OfficeInfo[A] =
    new OfficeInfo[A] {
      def name = _name
    }

}

trait OfficeInfo[A] {
  def name: String
  def isSagaOffice: Boolean = false
}