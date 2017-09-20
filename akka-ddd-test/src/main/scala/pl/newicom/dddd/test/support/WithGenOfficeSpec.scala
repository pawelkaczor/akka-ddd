package pl.newicom.dddd.test.support

import org.scalacheck.Gen
import pl.newicom.dddd.aggregate.Command

import scala.annotation.tailrec

trait WithGenOfficeSpec {

  @tailrec
  implicit final def arbitraryToSample[C <: Command](g: Gen[C]): C = {
    g.sample match {
      case Some(x) => x
      case _ => arbitraryToSample(g)
    }
  }

  def a[C <: Command](implicit g: Gen[C]): Gen[C] = g

  def a_list_of[C1 <: Command, C2 <: Command, C3 <: Command](implicit g1: Gen[C1], g2: Gen[C2], g3: Gen[C3]): List[Command] = List(g1, g2, g3)
  def a_list_of[C1 <: Command, C2 <: Command, C3 <: Command, C4 <: Command](implicit g1: Gen[C1], g2: Gen[C2], g3: Gen[C3], g4: Gen[C4]): List[Command] = List(g1, g2, g3, g4)
  def a_list_of[C1 <: Command, C2 <: Command](implicit g1: Gen[C1], g2: Gen[C2]): List[Command] = List(g1, g2)

  def arbitraryOf[C <: Command](adjust: (C) => C = {x: C => x})(implicit g: Gen[C]): C = adjust(g)
}
