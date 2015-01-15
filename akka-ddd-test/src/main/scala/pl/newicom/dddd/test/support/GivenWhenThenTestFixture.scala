package pl.newicom.dddd.test.support

import java.util.UUID

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.scalacheck.Gen
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.delivery.protocol.Acknowledged

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.ClassTag

abstract class GivenWhenThenTestFixture(_system: ActorSystem) extends TestKit(_system) with ImplicitSender {

  implicit def toAcks(acks: Seq[Acknowledged]): Acks = Acks(acks)

  case class Acks(list: Seq[Acknowledged] = List.empty) {
    private val map: Map[Class[_], Any] = list.map(a => (a.msg.getClass, a.msg)).toMap

    def get[E](implicit ct: ClassTag[E]): E = map.get(ct.runtimeClass).getOrElse(null).asInstanceOf[E]
  }

  def officeUnderTest: ActorRef

  case class WhenCommand[C <: Command](actual: C, acks: Acks = Acks(), params: Seq[Any] = Seq.empty)

  val fakeWhenCommand = WhenCommand(new Command {
    override def aggregateId: String = UUID.randomUUID().toString
  })

  @tailrec
  implicit final def toWhenCommand[C <: Command](cGen: Gen[C]): WhenCommand[C] = {
    cGen.sample match {
      case Some(x) => toWhenCommand(x)
      case _ => toWhenCommand[C](cGen)
    }
  }

  implicit def toWhenCommand[C <: Command](c: C): WhenCommand[C] = WhenCommand(c)
  implicit def toWhenCommandGen[C <: Command](cGen: Gen[(C, Any)]): WhenCommand[C] = {
    val (c, param1) = cGen.sample.get
    WhenCommand(c, Acks(), List(param1))
  }
  implicit def toCommand[C <: Command](c: WhenCommand[C]): C = c.actual

  case class Given(givenFun: () => Acks) {
    val acks = givenFun()

    def whenCommand[C <: Command](f: (Acks) => C): When[C] = whenCommand(f(acks))

    def whenCommand[C <: Command](command: WhenCommand[C]): When[C] = when(command, () => {
      officeUnderTest ! command.actual
    })

    private def when[C <: Command](command: WhenCommand[C], whenFun: () => Unit): When[C] = {
      When(command.copy(acks = acks), whenFun)
    }
  }

  def givenCommand(command: Command): Given = givenCommands(List(command) :_*)

  def givenCommands(commands: Command*) = {
    import akka.pattern.ask
    implicit val timeout = Timeout(5.seconds)

    Given(
      givenFun = () => {
        commands.map { c =>
          Await.result((officeUnderTest ? c).mapTo[Acknowledged], timeout.duration)
        }
      }
    )
  }

  case class When[C <: Command](command: WhenCommand[C], whenFun: () => Unit) {

    def expectEvent2[E](f: (WhenCommand[C]) => E)(implicit t: ClassTag[E]): Unit = {
      expectEvent(f(command))
    }

    def expectEvent3[E](f: (C, Any) => E)(implicit t: ClassTag[E]): Unit = {
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

  def whenCommand[C <: Command](c: WhenCommand[C]) = Given(() => Acks()).whenCommand(c)

  def when(whenFun: => Unit) = When(fakeWhenCommand, () => whenFun)
}
