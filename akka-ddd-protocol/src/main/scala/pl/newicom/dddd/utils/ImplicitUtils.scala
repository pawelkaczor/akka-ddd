package pl.newicom.dddd.utils
import scala.language.higherKinds

object ImplicitUtils {

  implicit final class AsParameterizedBy[F[_]](val f: F[_]) extends AnyVal {
    def asParameterizedBy[T]: F[T] = f.asInstanceOf[F[T]]
  }

  implicit final class AsParameterizedBy2[T, F[_, _]](val f: F[T, _]) extends AnyVal {
    def asParameterizedBy2[U]: F[T, U] = f.asInstanceOf[F[T, U]]
  }

}
