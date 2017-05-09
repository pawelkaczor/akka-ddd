package pl.newicom.dddd.test.support

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.scalacheck.Gen
import pl.newicom.dddd.aggregate.{Command, DomainEvent}
import pl.newicom.dddd.aggregate.error.CommandRejected
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.OfficeEventMessage
import pl.newicom.dddd.office.Office
import pl.newicom.dddd.office.SimpleOffice.Batch
import pl.newicom.dddd.utils.UUIDSupport._

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

abstract class GivenWhenThenTestFixture(_system: ActorSystem) extends TestKit(_system) with ImplicitSender {

  val timeoutCommand = Timeout(5.seconds)
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
    private val map: Map[Class[_], List[DomainEvent]] =
      list.flatMap(_.result.get.asInstanceOf[Seq[OfficeEventMessage]]).map(_.payload).groupBy(_.getClass)

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
      expectCommandsProcessed(wc.commands).map(_.result).flatMap {
        case Success(eventMsgs) => eventMsgs.asInstanceOf[Seq[OfficeEventMessage]].map(em => Success(em.payload))
        case f => Seq(f)
      }.foreach(_system.eventStream.publish)
    })

    private def when[C <: Command](wc: WhenContext[C], whenFun: () => Unit): When[C] = {
      When(wc.copy(pastEvents = pastEvents), whenFun)
    }
  }

  case class When[C <: Command](wc: WhenContext[C], whenFun: () => Unit) {

    def expectEvents(events: DomainEvent*): Unit = {
      val probe = testProbe(whenFun)
      events.foreach { _ =>
        probe.expectMsgAnyOf[DomainEvent](timeoutThen.duration, events.map(Success(_)): _*)
      }
    }

    def expectEvent(e: DomainEvent): Unit = {
      expectEventMatching(
        matcher = {
          case actual if actual == e => e
        },
        s"Success($e)"
      )
    }

    def expect(f: (WhenContext[C]) => DomainEvent): Unit = {
      expectEvent(f(wc))
    }

    def expect2(f: (C, Any) => DomainEvent): Unit = {
      expectEvent(f(wc.command, wc.params.head))
    }

    def expectException[E <: CommandRejected](message: String = null)(implicit t: ClassTag[E]): Unit = {
      testProbe(whenFun).expectMsgPF[Boolean](timeoutThen.duration, hint = s"Failure caused by ${t.runtimeClass.getName} with message $message") {
        case Failure(ex) if ex.getClass == t.runtimeClass && (message == null || message == ex.getMessage) => true
      }
    }

    def expectEventMatching2(f: (C) => PartialFunction[Any, Any], hint: String = ""): Any = {
      expectEventMatching(f(wc.command))
    }

    def expectEventMatching(matcher: PartialFunction[Any, Any], hint: String = ""): Any = {
      testProbe(whenFun).expectMsgPF[Any](timeoutThen.duration, hint) {
        case Success(result) if matcher.isDefinedAt(result) => result
      }
    }
  }

  def expectCommandsProcessed(cs: Seq[Command]): Seq[Processed] = {
    officeUnderTest.actor ! Batch(cs.map(commandMessage))
    expectMsgAllClassOf(timeoutCommand.duration, cs.map(_ => classOf[Processed]) :_*)
  }

  def given(cs: List[Command]): Given = given(cs :_*)

  def given(cs: Command*): Given = {
    Given(
      givenFun = () => expectCommandsProcessed(cs)
    )
  }

  def when[C <: Command](wc: WhenContext[C]): When[C] = Given(() => PastEvents()).when(wc)

  def whenF(whenFun: => Unit): When[_] = {
    When(fakeWhenContext(), () => whenFun)
  }

  def last[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E = past

  def past[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.last[E]

  def first[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.first[E]

  private def commandMessage(c: Command) = {
    CommandMessage(c).withMetaData(commandMetaDataProvider(c))
  }

  def commandMetaDataProvider(c: Command): Option[MetaData] = None


  private def testProbe(f: () => Unit) = {
    new TestProbe(_system) {
      var initialized = false

      def initialize(): Unit = {
        _system.eventStream.subscribe(this.ref, classOf[Success[_]])
        _system.eventStream.subscribe(this.ref, classOf[Failure[_]])
        f()
      }

      override def receiveOne(max: Duration): AnyRef = {
        if (!initialized) {
          initialize(); initialized = true
        }
        super.receiveOne(max)
      }
    }
  }

}
