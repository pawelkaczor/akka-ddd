package pl.newicom.dddd.test.ar

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.danielasfregola.randomdatagenerator.RandomDataGenerator
import org.scalacheck.Arbitrary
import pl.newicom.dddd.aggregate.error.CommandRejected
import pl.newicom.dddd.aggregate.{AggregateId, Command, DomainEvent}
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.OfficeEventMessage
import pl.newicom.dddd.office.OfficeRef
import pl.newicom.dddd.office.SimpleOffice.Batch
import pl.newicom.dddd.test.ar.GivenWhenThenARTestFixture.{Commands, CommandsHandler, ExpectedEvents, PastEvents, WhenContext, testProbe}
import pl.newicom.dddd.utils.UUIDSupport.uuid

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
  * Given
  */
case class Given(cs: Seq[Command] = Seq.empty)(implicit s: ActorSystem, ch: CommandsHandler, timeout: FiniteDuration) {
  val pastEvents: PastEvents = PastEvents(ch(cs).toList)

  def when[E <: DomainEvent, C <: Command](f: (WhenContext[_]) => WhenContext[C]): When[E, C] =
    when(f(fakeWhenContext(pastEvents)))

  def when[E <: DomainEvent, C <: Command](wc: WhenContext[C]): When[E, C] = when(wc, () => {
    ch(wc.commands).map(_.result).flatMap {
      case Success(eventMsgs) =>
        eventMsgs.asInstanceOf[Seq[OfficeEventMessage]].map(em => Success(em.payload))
      case f => Seq(f)
    }.foreach(s.eventStream.publish)
  })

  private def when[E <: DomainEvent, C <: Command](wc: WhenContext[C], whenFun: () => Unit): When[E, C] = {
    When(wc.copy(pastEvents = pastEvents), whenFun)
  }

  private def fakeWhenContext(pastEvents: PastEvents = PastEvents()) = WhenContext(Seq(new Command {
    override def aggregateId: AggregateId = AggregateId(uuid)
  }), pastEvents)

}

/**
  * When
  */
case class When[E <: DomainEvent, C <: Command](wc: WhenContext[C], whenFun: () => Unit)(implicit s: ActorSystem, timeout: FiniteDuration) {

  def expectEvents(events: E*): Unit = {
    val probe = testProbe(whenFun)
    events.foreach { _ =>
      probe.expectMsgAnyOf[DomainEvent](timeout, events.map(Success(_)): _*)
    }
  }

  def expectEvent(e: E): Unit = {
    expectEventMatching(
      matcher = {
        case actual if actual == e => e
      },
      s"Success($e)"
    )
  }

  def expect(f: (WhenContext[C]) => ExpectedEvents[E]): Unit =
    f(wc) match {
      case ExpectedEvents(Seq(e)) =>
        expectEvent(e)
      case ExpectedEvents(events) =>
        expectEvents(events :_*)
    }

  def expect2(f: (C, Any) => E): Unit = {
    expectEvent(f(wc.command, wc.params.head))
  }

  def expectCommandRejected: Unit = {
    testProbe(whenFun).expectMsgPF[Boolean](timeout) {
      case Failure(_: CommandRejected)  => true
    }
  }

  def expectException[CR <: CommandRejected](message: String = null)(implicit t: ClassTag[CR]): Unit = {
    testProbe(whenFun).expectMsgPF[Boolean](timeout, hint = s"Failure caused by ${t.runtimeClass.getName} with message $message") {
      case Failure(ex) if ex.getClass == t.runtimeClass && (message == null || message == ex.getMessage) => true
    }
  }

  def expectEventMatching2(f: (C) => PartialFunction[Any, Any], hint: String = ""): Any = {
    expectEventMatching(f(wc.command))
  }

  def expectEventMatching(matcher: PartialFunction[Any, Any], hint: String = ""): Any = {
    testProbe(whenFun).expectMsgPF[Any](timeout, hint) {
      case Success(result) if matcher.isDefinedAt(result) => result
    }
  }
}

/**
  * Fixture
  */
object GivenWhenThenARTestFixture {

  type CommandsHandler = Seq[Command] => Seq[Processed]

  case class Commands[C <: Command](commands: Seq[C]) {
    def &(c: C): Commands[C] = Commands[C](commands :+ c)
  }

  case class WhenContext[C <: Command](
                                        commands: Seq[C],
                                        pastEvents: PastEvents = PastEvents(),
                                        params: Seq[Any] = Seq.empty) {
    def command: C = commands.head

    def &(c: C): WhenContext[C] = copy(commands = commands :+ c)
  }

  case class PastEvents(list: List[Processed] = List.empty) {
    private val map: Map[Class[_], List[DomainEvent]] =
      list.flatMap(_.result.get.asInstanceOf[Seq[OfficeEventMessage]]).map(_.payload).groupBy(_.getClass)

    private def event[E](selection: List[E] => E)(implicit ct: ClassTag[E]) =
      map.get(ct.runtimeClass)
        .map(es => es.asInstanceOf[List[E]]).map(selection)
        .getOrElse(null.asInstanceOf[E])

    def first[E](implicit ct: ClassTag[E]): E = event[E](_.head)
    def last[E](implicit ct: ClassTag[E]): E = event[E](_.last)
  }

  case class ExpectedEvents[E](events: Seq[E]) {
    def &(e: E): ExpectedEvents[E] = ExpectedEvents(events :+ e)
  }

  def testProbe(f: () => Unit)(implicit system: ActorSystem): TestProbe = {
    new TestProbe(system) {
      var initialized = false

      def initialize(): Unit = {
        system.eventStream.subscribe(this.ref, classOf[Success[_]])
        system.eventStream.subscribe(this.ref, classOf[Failure[_]])
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

  implicit def whenContextToCommand[C <: Command](wc: WhenContext[C]): C = wc.command

  implicit def whenContextToPastEvents[C <: Command](wc: WhenContext[C]): PastEvents = wc.pastEvents

  implicit def commandToWhenContext[C <: Command](c: C): WhenContext[C] = WhenContext(Seq(c))

  implicit def commandsToWhenContext[C <: Command](cs: Seq[C]): WhenContext[C] = WhenContext(cs)

  implicit def commandsToWhenContext[C <: Command](cs: Commands[C]): WhenContext[C] = WhenContext[C](cs.commands)


  implicit final def aCmdToWhenContext[C <: Command](cmd: Arbitrary[C]): WhenContext[C] =
    commandToWhenContext(RandomDataGenerator.random(cmd))

  implicit def aCmdWithParamToWhenContext[C <: Command](cmd: Arbitrary[(C, Any)]): WhenContext[C] = {
    val (c, param1) = RandomDataGenerator.random(cmd)
    WhenContext(Seq(c), PastEvents(), List(param1))
  }

}

abstract class GivenWhenThenARTestFixture[Event <: DomainEvent](_system: ActorSystem) extends TestKit(_system) with ImplicitSender {

  implicit val timeout: FiniteDuration = Timeout(5.seconds).duration

  def officeUnderTest: OfficeRef

  def ensureOfficeTerminated(): Unit

  // DSL

  def given(cs: List[Command]): Given = given(cs :_*)

  def given(cs: Command*): Given = Given(cs)

  def given[C <: Command](cs: Commands[C]): Given = Given(cs.commands)

  def when[C <: Command](wc: WhenContext[C]): When[Event, C] = Given().when(wc)

  def last[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E = past

  def past[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.last[E]

  def first[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.first[E]

  protected def commandMetaDataProvider(c: Command): MetaData = MetaData.empty

  implicit def toExpectedEvents(e: Event): ExpectedEvents[Event] =
    ExpectedEvents(Seq(e))

  implicit def toCommands(c: Command): Commands[Command] =
    Commands(Seq(c))

  // Private methods

  private implicit def commandsHandler: CommandsHandler = {
    def cm(c: Command) =
      CommandMessage(c).withMetaData(commandMetaDataProvider(c))

    (cs: Seq[Command]) =>
      if (cs.isEmpty) {
        Seq.empty
      } else {
        officeUnderTest.actor ! Batch(cs.map(cm))
        expectMsgAllClassOf(timeout, cs.map(_ => classOf[Processed]): _*)
      }
  }.andThen(r => { if (r.nonEmpty) { ensureOfficeTerminated() }; r})

}
