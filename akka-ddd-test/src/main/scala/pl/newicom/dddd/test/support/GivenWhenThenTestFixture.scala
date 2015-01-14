package pl.newicom.dddd.test.support

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalacheck.Gen
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.Acknowledged

import scala.concurrent.duration._
import scala.reflect.ClassTag

abstract class GivenWhenThenTestFixture(_system: ActorSystem) extends TestKit(_system) with ImplicitSender {

  def officeUnderTest: ActorRef

  case class WhenCommand[C <: Command](actual: C, params: Seq[Any] = Seq.empty)

  implicit def toWhenCommand[C <: Command](cGen: Gen[C]): WhenCommand[C] = toWhenCommand(cGen.sample.get)
  implicit def toWhenCommand[C <: Command](c: C): WhenCommand[C] = WhenCommand(c)
  implicit def toWhenCommandGen[C <: Command](cGen: Gen[(C, Any)]): WhenCommand[C] = {
    val (c, param1) = cGen.sample.get
    WhenCommand(c, List(param1))
  }

  case class Given(givenFun: () => Unit, givenCompleted: () => Boolean = () => true) {

    def whenCommand[C <: Command](command: WhenCommand[C]): When[C] = when(command, () => {
      officeUnderTest ! command.actual
    })

    private def when[C <: Command](command: WhenCommand[C], whenFun: () => Unit): When[C] = {
      givenFun()
      if (givenCompleted()) {
        When(command, whenFun)
      } else {
        throw new RuntimeException("Given failed")
      }
    }
  }

  def givenCommand(command: Any): Given = givenCommands(List(command) :_*)

  def givenCommands(commands: Any*) = {
    def commandsAcknowledged: Boolean = {
      expectMsgAllOf(commands.map(_ => Acknowledged) :_*)
      true
    }

    Given(
      givenFun = () => { commands.foreach { officeUnderTest ! _ }},
      givenCompleted = () => commandsAcknowledged
    )
  }

  case class When[C <: Command](command: WhenCommand[C], whenFun: () => Unit) {

    def expectEvent[E](f: (C) => E)(implicit t: ClassTag[E]): Unit = {
      expectEvent(f(command.actual))
    }

    def expectEvent[E](f: (C, Any) => E)(implicit t: ClassTag[E]): Unit = {
      expectEvent(f(command.actual, command.params(0)))
    }

    def expectEvent[E](e: E)(implicit t: ClassTag[E]): Unit =
      expectEventMatching[E](
        matcher = {
          case actual
            if actual == e => e
        },
        hint = e.toString
      )

    def expectException[E <: Exception](message: String = null)(implicit t: ClassTag[E]): Unit = {
      whenFun()
      expectMsgPF[Boolean](3 seconds, hint = s"Failure caused by ${t.runtimeClass.getName} with message $message") {
        case actual @ Failure(ex) if ex.getClass == t.runtimeClass && (message == null || message == ex.getMessage) => true
      }
    }

    def expectEventMatching2[E](f: (C) => PartialFunction[Any, E], hint: String = "")(implicit t: ClassTag[E]): E = {
      expectEventMatching(f(command.actual))
    }

    def expectEventMatching[E](matcher: PartialFunction[Any, E], hint: String = "")(implicit t: ClassTag[E]): E = {
      val probe = TestProbe()
      _system.eventStream.subscribe(probe.ref, t.runtimeClass)
      whenFun()
      probe.expectMsgPF[E](3 seconds, hint)(matcher)
    }

  }

  def whenCommand[C <: Command](c: WhenCommand[C]) = Given(() => ()).whenCommand(c)
}
