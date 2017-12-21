package pl.newicom.dddd.test.pm

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.{Command, DomainEvent}
import pl.newicom.dddd.delivery.protocol.alod.Processed
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.SimpleOffice.Batch
import pl.newicom.dddd.office.{OfficeRef, OfficeRegistry, RemoteOfficeId}
import pl.newicom.dddd.scheduling.ScheduleEvent
import pl.newicom.dddd.test.pm.GivenWhenThenPMTestFixture.{Events, EventsHandler, PastEvents, WhenContext}

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.Success
import akka.pattern.ask

import scala.concurrent.Await

object TestCommandOfficeId extends RemoteOfficeId[Command]("Default Command Office", "tests", classOf[Command])
case object Init

/**
  * Given
  */
case class Given(es: Seq[DomainEvent] = Seq.empty)(implicit s: ActorSystem, eh: EventsHandler, timeout: FiniteDuration) {

  private val commandListener = s.actorOf(Props(new Actor {
    def receive: Receive = {
      case Init =>
        context.become(ready)
        sender() ! "Ack"
      case _ => // ignore
    }

    def ready: Receive = {
      case CommandMessage(c, _) => s.eventStream.publish(c)
    }
  }))

  OfficeRegistry(s).registerOffice(new OfficeRef(TestCommandOfficeId, commandListener))

  val pastEvents: PastEvents = PastEvents(eh(es).toList)

  def when[E <: DomainEvent](f: (WhenContext[_]) => WhenContext[E]): When[E] =
    when(f(fakeWhenContext(pastEvents)))

  def when[E <: DomainEvent](wc: WhenContext[E]): When[E] = when(wc, () => {
    implicit val t: Timeout = Timeout(timeout)
    Await.ready(commandListener ? Init, timeout)
    eh(Seq(wc.event)).map(_.payload).map(Success(_)).foreach(s.eventStream.publish)
  })

  private def when[E <: DomainEvent](wc: WhenContext[E], whenFun: () => Unit): When[E] = {
    When(wc.copy(pastEvents = pastEvents), whenFun)
  }

  private def fakeWhenContext(pastEvents: PastEvents = PastEvents()) =
    WhenContext(Seq(new DomainEvent), pastEvents)

}

/**
  * When
  */
case class When[E <: DomainEvent](wc: WhenContext[E], whenFun: () => Unit)(implicit s: ActorSystem, timeout: FiniteDuration) {
  private val whenExecuted = new AtomicBoolean(false)
  private val commandTestProbe = testProbe(classOf[Command])
  private val eventTestProbe = testProbe(classOf[Success[_]])

  def expectEvents(events: DomainEvent*): AfterWhen[E] = {
    val probe = eventTestProbe
    events.foreach { _ =>
      probe.expectMsgAnyOf[DomainEvent](timeout, events.map(Success(_)): _*)
    }
    AfterWhen(wc, commandTestProbe)
  }

  def expectEvent(e: DomainEvent): AfterWhen[E] = {
    expectEventMatching(
      matcher = {
        case actual if actual == e => e
      },
      s"Success($e)"
    )
    AfterWhen(wc, commandTestProbe)
  }

  def expectEventMatching(matcher: PartialFunction[Any, Any], hint: String = ""): Any = {
    eventTestProbe.expectMsgPF[Any](timeout, hint) {
      case Success(result) if matcher.isDefinedAt(result) => result
    }
  }

  def expectReceivedEvent: AfterWhen[E] =
    expectEvent(wc.event)

  def expect(f: (WhenContext[E]) => Events): AfterWhen[E] =
    f(wc) match {
      case Events(Seq(e)) =>
        expectEvent(e)
      case Events(events) =>
        expectEvents(events :_*)
    }

  private def testProbe(channel: Class[_]): TestProbe = {
    new TestProbe(s) {

      system.eventStream.subscribe(this.ref, channel)

      override def receiveOne(max: Duration): AnyRef = {
        if (whenExecuted.compareAndSet(false, true)) {
          whenFun()
        }
        super.receiveOne(max)
      }
    }
  }
}

case class AfterWhen[E <: DomainEvent](wc: WhenContext[E], testProbe: TestProbe)(implicit s: ActorSystem, timeout: FiniteDuration) {

  def expectCommand(c: Command): AfterWhen[E] = {
    testProbe.expectMsg(timeout, c)
    this
  }

  def expect(f: (WhenContext[E]) => Command): AfterWhen[E] = {
    expectCommand(f(wc))
    this
  }

  def expectScheduled(deadline: DateTime)(f: (WhenContext[E]) => DomainEvent): Unit =
    expectEventScheduled(deadline)(f(wc))

  def expectEventScheduled(deadline: DateTime)(e: DomainEvent): Unit =
    expectCommandMatching(
      matcher = {
        case ScheduleEvent(_, _, actualDeadline, actualEvent)  if actualDeadline == deadline && e == actualEvent => true
      },
      s"ScheduleEvent(_, _, $deadline, $e)"
    )

  def expectCommandMatching(matcher: PartialFunction[Command, Any], hint: String = ""): Command = {
    testProbe.expectMsgPF[Command](timeout, hint) {
      case c: Command if matcher.isDefinedAt(c) => c
    }
  }

}


/**
  * Fixture
  */
object GivenWhenThenPMTestFixture {

  type EventsHandler = Seq[DomainEvent] => Seq[EventMessage]

  case class Events(events: Seq[DomainEvent]) {
    def &(c: DomainEvent): Events = Events(events :+ c)
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

  def given(es: Events): Given = Given(es.events)

  def given(es: DomainEvent*): Given = Given(es)

  def when[E <: DomainEvent](wc: WhenContext[E]): When[E] = Given().when(wc)

  def last[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E = past

  def past[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.last[E]

  def first[E](implicit wc: WhenContext[_], ct: ClassTag[E]): E =
    wc.pastEvents.first[E]

  protected def eventMetaDataProvider(e: DomainEvent): MetaData

  implicit def toEvents(e: DomainEvent): Events =
    Events(Seq(e))

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
  }

}
