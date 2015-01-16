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

  def officeUnderTest: ActorRef

  implicit def whenContextToCommand[C <: Command](wc: WhenContext[C]): C = wc.command

  implicit def whenContextToPastEvents[C <: Command](wc: WhenContext[C]): PastEvents = wc.pastEvents

  implicit def commandToWhenContext[C <: Command](c: C): WhenContext[C] = WhenContext(c)

  @tailrec
  implicit final def commandGenToWhenContext[C <: Command](cGen: Gen[C]): WhenContext[C] = {
    cGen.sample match {
      case Some(x) => commandToWhenContext(x)
      case _ => commandGenToWhenContext[C](cGen)
    }
  }

  implicit def commandGenWithParamToWhenContext[C <: Command](cGen: Gen[(C, Any)]): WhenContext[C] = {
    val (c, param1) = cGen.sample.get
    WhenContext(c, PastEvents(), List(param1))
  }

  implicit def acksToPastEvents(acks: Seq[Acknowledged]): PastEvents = PastEvents(acks)

  case class PastEvents(list: Seq[Acknowledged] = List.empty) {
    private val map: Map[Class[_], Any] = list.map(a => (a.msg.getClass, a.msg)).toMap

    def get[E](implicit ct: ClassTag[E]): E = map.getOrElse(ct.runtimeClass, null).asInstanceOf[E]
  }

  case class WhenContext[C <: Command](
    command: C,
    pastEvents: PastEvents = PastEvents(),
    params: Seq[Any] = Seq.empty)

  case class Given(givenFun: () => PastEvents) {
    val pastEvents = givenFun()

    def whenCommand[C <: Command](f: (PastEvents) => WhenContext[C]): When[C] = whenCommand(f(pastEvents))

    def whenCommand[C <: Command](wc: WhenContext[C]): When[C] = when(wc, () => {
      officeUnderTest ! wc.command
    })

    private def when[C <: Command](wc: WhenContext[C], whenFun: () => Unit): When[C] = {
      When(wc.copy(pastEvents = pastEvents), whenFun)
    }
  }

  case class When[C <: Command](wc: WhenContext[C], whenFun: () => Unit) {

    def expectEvent[E](e: E)(implicit t: ClassTag[E]): Unit = {
      expectEventMatching[E](
        matcher = {
          case actual
            if actual == e => e
        },
        hint = e.toString
      )
    }

    def expectEvent2[E](f: (WhenContext[C]) => E)(implicit t: ClassTag[E]): Unit = {
      expectEvent(f(wc))
    }

    def expectEvent3[E](f: (C, Any) => E)(implicit t: ClassTag[E]): Unit = {
      expectEvent(f(wc.command, wc.params(0)))
    }

    def expectException[E <: Exception](message: String = null)(implicit t: ClassTag[E]): Unit = {
      whenFun()
      expectMsgPF[Boolean](3 seconds, hint = s"Failure caused by ${t.runtimeClass.getName} with message $message") {
        case actual @ Failure(ex) if ex.getClass == t.runtimeClass && (message == null || message == ex.getMessage) => true
      }
    }

    def expectEventMatching2[E](f: (C) => PartialFunction[Any, E], hint: String = "")(implicit t: ClassTag[E]): E = {
      expectEventMatching(f(wc.command))
    }

    def expectEventMatching[E](matcher: PartialFunction[Any, E], hint: String = "")(implicit t: ClassTag[E]): E = {
      val probe = TestProbe()
      _system.eventStream.subscribe(probe.ref, t.runtimeClass)
      whenFun()
      probe.expectMsgPF[E](3 seconds, hint)(matcher)
    }

  }

  def givenCommand(c: Command): Given = givenCommands(List(c) :_*)

  def givenCommands(c: Command*) = {
    import akka.pattern.ask
    implicit val timeout = Timeout(5.seconds)

    Given(
      givenFun = () => {
        c.map { c =>
          Await.result((officeUnderTest ? c).mapTo[Acknowledged], timeout.duration)
        }
      }
    )
  }

  def whenCommand[C <: Command](wc: WhenContext[C]) = Given(() => PastEvents()).whenCommand(wc)

  def when(whenFun: => Unit) = {
    val fakeWhenCommand = WhenContext(new Command {
      override def aggregateId: String = UUID.randomUUID().toString
    })

    When(fakeWhenCommand, () => whenFun)
  }

  def past[E](implicit pastEvents: PastEvents, classTag: ClassTag[E]): E = pastEvents.get[E]

}
