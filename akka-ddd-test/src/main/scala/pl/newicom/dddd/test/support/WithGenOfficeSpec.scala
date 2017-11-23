package pl.newicom.dddd.test.support

import com.danielasfregola.randomdatagenerator.RandomDataGenerator
import org.scalacheck.{Arbitrary, Gen}
import pl.newicom.dddd.aggregate.Command

trait DefaultGenerators {

  implicit val arbitraryString: Arbitrary[String] = Arbitrary(Gen.alphaStr)
}

trait WithGenOfficeSpec extends DefaultGenerators {

  type A[T] = Arbitrary[T]

  implicit final def arbitraryToSample[C <: Command](a: A[C]): C =
    RandomDataGenerator.random(a)

  def a[C <: Command](implicit a: A[C]): A[C] = a

  def a_list_of[C1 <: Command, C2 <: Command, C3 <: Command](implicit g1: A[C1], g2: A[C2], g3: A[C3]): List[Command] = List(g1, g2, g3)
  def a_list_of[C1 <: Command, C2 <: Command, C3 <: Command, C4 <: Command](implicit g1: A[C1], g2: A[C2], g3: A[C3], g4: A[C4]): List[Command] = List(g1, g2, g3, g4)
  def a_list_of[C1 <: Command, C2 <: Command](implicit g1: A[C1], g2: A[C2]): List[Command] = List(g1, g2)

}
