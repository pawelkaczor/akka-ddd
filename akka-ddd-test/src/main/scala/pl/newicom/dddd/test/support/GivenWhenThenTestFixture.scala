package pl.newicom.dddd.test.support

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.scalacheck.Gen
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.aggregate.error.DomainException
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.office.Office
import pl.newicom.dddd.utils.UUIDSupport._

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.ClassTag

abstract class GivenWhenThenTestFixture(_system: ActorSystem) extends TestKit(_system) with ImplicitSender {

  val timeoutGiven = Timeout(5.seconds)
  val timeoutThen  = Timeout(5.seconds)

  def officeUnderTest: Office

  def ensureOfficeTerminated(): Unit

  private def fakeWhenContext(pastEvents: PastEvents = PastEvents()) = WhenContext(Seq(new Command {
    override def aggregateId: String = uuid
  }), pastEvents)
  
  implicit def whenContextToCommand[C <: Command](wc: WhenContext[C]): C = wc.command

  implicit def whenContextToPastEvents[C <: Command](wc: WhenContext[C]): PastEvents = wc.pastEvents

  implicit def commandToWhenContext[C <: Command](c: C): WhenContext[C] = WhenContext(Seq(c))
  implicit def commandsToWhenContext[C <: Command](cs: Seq[C]): WhenContext[C] = WhenContext(cs)

  @tailrec
  implicit final def commandGenToWhenContext[C <: Command](cGen: Gen[C]): WhenContext[C] = {
    cGen.sample match {
      case Some(x) => commandToWhenContext(x)
      case _ => commandGenToWhenContext[C](cGen)
    }
  }

  implicit def commandGenWithParamToWhenContext[C <: Command](cGen: Gen[(C, Any)]): WhenContext[C] = {
    val (c, param1) = cGen.sample.get
    WhenContext(Seq(c), PastEvents(), List(param1))
  }

  implicit def acksToPastEvents(acks: Seq[Processed]): PastEvents = PastEvents(acks.toList)

  case class PastEvents(list: List[Processed] = List.empty) {
    private val map: Map[Class[_], List[Any]] =
      list.groupBy(_.result.get.getClass).mapValues(ackSeq => ackSeq.map(_.result.get))

    def first[E](implicit ct: ClassTag[E]): E = map.get(ct.runtimeClass).map(_.head).orNull.asInstanceOf[E]
    def last[E](implicit ct: ClassTag[E]): E = map.get(ct.runtimeClass).map(_.last).orNull.asInstanceOf[E]
  }

  case class WhenContext[C <: Command](
    commands: Seq[C],
    pastEvents: PastEvents = PastEvents(),
    params: Seq[Any] = Seq.empty) {
    def command: C = commands.head
  }

  case class Given(givenFun: () => PastEvents) {
    val pastEvents: PastEvents = givenFun()
    ensureOfficeTerminated()

    def when[C <: Command](f: (WhenContext[_]) => WhenContext[C]): When[C] =
      when(f(fakeWhenContext(pastEvents)))

    def when[C <: Command](wc: WhenContext[C]): When[C] = when(wc, () => {
      wc.commands.foreach { command =>
        val cm = CommandMessage(command).withMetaData(commandMetaDataProvider(command))
        if (wc.commands.size > 1) {
          import akka.pattern.ask
          implicit val timeout = timeoutGiven
          Await.ready(officeUnderTest.actor ? cm, timeoutGiven.duration)
        } else {
          officeUnderTest.actor ! cm
        }
      }
    })

    private def when[C <: Command](wc: WhenContext[C], whenFun: () => Unit): When[C] = {
      When(wc.copy(pastEvents = pastEvents), whenFun)
    }
  }

  case class When[C <: Command](wc: WhenContext[C], whenFun: () => Unit) {

    def expectEvents[E](events: E*)(implicit t: ClassTag[E]): Unit = {
      val probe = TestProbe()
      _system.eventStream.subscribe(probe.ref, t.runtimeClass)
      whenFun()
      events.foreach { _ =>
        probe.expectMsgAnyOf[E](timeoutThen.duration, events: _*)
      }
    }

    def expectEvent[E](e: E)(implicit t: ClassTag[E]): Unit = {
      expectEventMatching[E](
        matcher = {
          case actual if actual == e => e
        },
        hint = e.toString
      )
    }

    def expect[E](f: (WhenContext[C]) => E)(implicit t: ClassTag[E]): Unit = {
      expectEvent(f(wc))
    }

    def expect2[E](f: (C, Any) => E)(implicit t: ClassTag[E]): Unit = {
      expectEvent(f(wc.command, wc.params.head))
    }

    def expectException[E <: DomainException](message: String = null)(implicit t: ClassTag[E]): Unit = {
      whenFun()
      expectMsgPF[Boolean](timeoutThen.duration, hint = s"Failure caused by ${t.runtimeClass.getName} with message $message") {
        case Processed(scala.util.Failure(ex)) if ex.getClass == t.runtimeClass && (message == null || message == ex.getMessage) => true
      }
    }

    def expectEventMatching2[E](f: (C) => PartialFunction[Any, E], hint: String = "")(implicit t: ClassTag[E]): E = {
      expectEventMatching(f(wc.command))
    }

    def expectEventMatching[E](matcher: PartialFunction[Any, E], hint: String = "")(implicit t: ClassTag[E]): E = {
      val probe = TestProbe()
      _system.eventStream.subscribe(probe.ref, t.runtimeClass)
      whenFun()
      probe.expectMsgPF[E](timeoutThen.duration, hint)(matcher)
    }

  }

  def given(cs: List[Command]): Given = given(cs :_*)

  def given(cs: Command*): Given = {
    import akka.pattern.ask
    implicit val timeout = timeoutGiven
    Given(
      givenFun = () => {
        cs.map { c =>
          val cm = CommandMessage(c).withMetaData(commandMetaDataProvider(c))
          Await.result((officeUnderTest.actor ? cm).mapTo[Processed], timeout.duration)
        }
      }
    )
  }

  def when[C <: Command](wc: WhenContext[C]): When[C] = Given(() => PastEvents()).when(wc)

  def whenF(whenFun: => Unit): When[_] = {
    When(fakeWhenContext(), () => whenFun)
  }

  def past[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.last[E]

  def first[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.first[E]

  def commandMetaDataProvider(c: Command): Option[MetaData] = None
}
