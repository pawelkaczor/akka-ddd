package pl.newicom.dddd.test.pm

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import pl.newicom.dddd.aggregate.{Command, DomainEvent}
import pl.newicom.dddd.delivery.protocol.alod.Processed
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.OfficeRef
import pl.newicom.dddd.office.SimpleOffice.Batch
import pl.newicom.dddd.test.pm.GivenWhenThenPMTestFixture.{EventsHandler, ExpectedEvents, PastEvents, WhenContext, testProbe}

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.Success

/**
  * Given
  */
case class Given(es: Seq[DomainEvent] = Seq.empty)(implicit s: ActorSystem, eh: EventsHandler, timeout: FiniteDuration) {
  val pastEvents: PastEvents = PastEvents(eh(es).toList)

  def when[E <: DomainEvent](f: (WhenContext[_]) => WhenContext[E]): When[E] =
    when(f(fakeWhenContext(pastEvents)))

  def when[E <: DomainEvent](wc: WhenContext[E]): When[E] = when(wc, () => {
    eh(Seq(wc.event)).map(_.payload).map(Success(_)).foreach(s.eventStream.publish)
  })

  private def when[E <: DomainEvent](wc: WhenContext[E], whenFun: () => Unit): When[E] = {
    When(wc.copy(pastEvents = pastEvents), whenFun)
  }

  private def fakeWhenContext(pastEvents: PastEvents = PastEvents()) = WhenContext(Seq(new DomainEvent), pastEvents)

}

/**
  * When
  */
case class When[E <: DomainEvent](wc: WhenContext[E], whenFun: () => Unit)(implicit s: ActorSystem, timeout: FiniteDuration) {

  def expectEvents(events: DomainEvent*): Unit = {
    val probe = testProbe(whenFun)
    events.foreach { _ =>
      probe.expectMsgAnyOf[DomainEvent](timeout, events.map(Success(_)): _*)
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

  def expect(f: (WhenContext[E]) => ExpectedEvents): Unit =
    f(wc) match {
      case ExpectedEvents(Seq(e)) =>
        expectEvent(e)
      case ExpectedEvents(events) =>
        expectEvents(events :_*)
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
object GivenWhenThenPMTestFixture {

  type EventsHandler = Seq[DomainEvent] => Seq[EventMessage]

  case class Commands[C <: Command](commands: Seq[C]) {
    def &(c: C): Commands[C] = Commands[C](commands :+ c)
  }

  case class WhenContext[E <: DomainEvent](
      event: E,
      pastEvents: PastEvents = PastEvents())

  case class PastEvents(list: List[EventMessage] = List.empty) {
    private val map: Map[Class[_], List[DomainEvent]] =
      list.map(_.payload).groupBy(_.getClass)

    private def event[E](selection: List[E] => E)(implicit ct: ClassTag[E]) =
      map.get(ct.runtimeClass)
        .map(es => es.asInstanceOf[List[E]]).map(selection)
        .getOrElse(null.asInstanceOf[E])

    def first[E](implicit ct: ClassTag[E]): E = event[E](_.head)
    def last[E](implicit ct: ClassTag[E]): E = event[E](_.last)
  }

  case class ExpectedEvents(events: Seq[DomainEvent]) {
    def &(e: DomainEvent): ExpectedEvents = ExpectedEvents(events :+ e)
  }

  def testProbe(f: () => Unit)(implicit system: ActorSystem): TestProbe = {
    new TestProbe(system) {
      var initialized = false

      def initialize(): Unit = {
        system.eventStream.subscribe(this.ref, classOf[Success[_]])
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

  implicit def whenContextToEvent[E <: DomainEvent](wc: WhenContext[E]): E = wc.event

  implicit def whenContextToPastEvents[E <: DomainEvent](wc: WhenContext[E]): PastEvents = wc.pastEvents

  implicit def eventToWhenContext[E <: DomainEvent](e: E): WhenContext[E] = WhenContext(e)

}

abstract class GivenWhenThenPMTestFixture(_system: ActorSystem) extends TestKit(_system) with ImplicitSender {

  implicit val timeout: FiniteDuration = Timeout(5.seconds).duration

  def officeUnderTest: OfficeRef

  def ensureOfficeTerminated(): Unit

  // DSL

  def given(es: List[DomainEvent]): Given = given(es :_*)

  def given(es: DomainEvent*): Given = Given(es)

  def when[E <: DomainEvent](wc: WhenContext[E]): When[E] = Given().when(wc)

  def last[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E = past

  def past[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.last[E]

  def first[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.first[E]

  protected def eventMetaDataProvider(e: DomainEvent): MetaData

  implicit def toExpectedEvents(e: DomainEvent): ExpectedEvents =
    ExpectedEvents(Seq(e))


  // Private methods

  private implicit def eventsHandler: EventsHandler = {
    def em(e: DomainEvent, deliveryId: Int) =
      EventMessage(e)
        .withMetaData(eventMetaDataProvider(e))
        .withDeliveryId(deliveryId)

    (es: Seq[DomainEvent]) =>
      if (es.isEmpty) {
        Seq.empty
      } else {
        val batch = Batch(es.map(e => em(e, es.indexOf(e) + 1)))
        officeUnderTest.actor ! batch
        expectMsgAllClassOf(timeout, es.map(_ => classOf[Processed]): _*).flatMap(_ => batch.msgs)
      }
  }.andThen(r => { if (r.nonEmpty) { ensureOfficeTerminated() }; r})

}
