package pl.newicom.dddd.test.support

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import pl.newicom.dddd.delivery.protocol.Acknowledged

import scala.concurrent.duration._
import scala.reflect.ClassTag

abstract class GivenWhenThenTestFixture(_system: ActorSystem) extends TestKit(_system) with ImplicitSender {

  def officeUnderTest: ActorRef

  case class Given(givenFun: () => Unit, givenCompleted: () => Boolean = () => true) {

    def whenCommand(command: AnyRef) = when(() => {
      officeUnderTest ! command
    })

    def when(whenFun: () => Unit) = {
      givenFun()
      if (givenCompleted()) {
        When(whenFun)
      } else {
        throw new RuntimeException("Given failed")
      }
    }
  }

  def givenCommand(command: Any) = givenCommands(List(command) :_*)

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

  case class When(whenFun: () => Unit) {

    def expectEvent[E](e: E)(implicit t: ClassTag[E]): Unit =
      expectEventMatching[E](
        matcher = { case actual if actual == e => e},
        hint = e.toString
      )

    def expectException[E <: Exception](message: String = null)(implicit t: ClassTag[E]): Unit = {
      whenFun()
      expectMsgPF[Boolean](3 seconds, hint = s"Failure caused by ${t.runtimeClass.getName} with message $message") {
        case actual @ Failure(ex) if ex.getClass == t.runtimeClass && (message == null || message == ex.getMessage) => true
      }
    }

    def expectEventMatching[E](matcher: PartialFunction[Any, E], hint: String = "")(implicit t: ClassTag[E]): E = {
      val probe = TestProbe()
      _system.eventStream.subscribe(probe.ref, t.runtimeClass)
      whenFun()
      probe.expectMsgPF[E](3 seconds, hint)(matcher)
    }

  }

  def when(whenFun: => Unit) = When(() => whenFun)

  def whenCommand(command: AnyRef) = Given(() => ()).whenCommand(command)
}
