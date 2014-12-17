package pl.newicom.dddd.test.support

import akka.actor._
import akka.testkit.{EventFilter, TestKitBase, TestProbe}
import akka.util.Timeout
import org.scalacheck.Gen
import org.scalatest.Suite
import pl.newicom.dddd.aggregate.EntityId

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.util.Failure

trait AggregateRootTestHelper {
  this: Suite with TestKitBase =>

  def system: ActorSystem
  def domain: String

  def expectEventPublishedMatching[E](matcher: PartialFunction[Any, Boolean])(implicit t: ClassTag[E]) {
    val probe = TestProbe()
    system.eventStream.subscribe(probe.ref, t.runtimeClass)
    assert(probe.expectMsgPF[Boolean](10 seconds)(matcher), s"unexpected event")

  }

  def expectEventPublished[E](implicit t: ClassTag[E]) {
    val probe = TestProbe()
    system.eventStream.subscribe(probe.ref, t.runtimeClass)
    probe.expectMsgClass(10 seconds, t.runtimeClass)
  }

  def expectEventPersisted[E](when: => Unit)(implicit aggregateIdGen: Gen[EntityId], t: ClassTag[E]): Unit = {
    expectEventPersisted[E](aggregateIdGen.sample.get)(when)
  }

  def expectEventPersisted[E](aggregateId: String)(when: => Unit)(implicit t: ClassTag[E]) {
    expectLogMessageFromAR("Event persisted: " + t.runtimeClass.getSimpleName, when)(aggregateId)
  }

  def expectEventPersisted[E](event: E)(aggregateRootId: String)(when: => Unit) {
    expectLogMessageFromAR("Event persisted: " + event.toString, when)(aggregateRootId)
  }

  def expectLogMessageFromAR(messageStart: String, when: => Unit)(aggregateId: String) {
    EventFilter.info(
      source = s"akka://Tests/user/$domain/$aggregateId",
      start = messageStart, occurrences = 1)
      .intercept {
        when
      }
  }

  def expectExceptionLogged[E <: Throwable](when: => Unit)(implicit t: ClassTag[E]) {
    EventFilter[E](occurrences = 1) intercept {
      when
    }
  }

  def expectLogMessageFromOffice(messageStart: String)(when: => Unit) {
    EventFilter.info(
      source = s"akka://Tests/user/$domain",
      start = messageStart, occurrences = 1)
      .intercept {
        when
      }
  }

  def expectFailure[E](awaitable: Future[Any])(implicit t: ClassTag[E]) {
    implicit val timeout = Timeout(5, SECONDS)
    val future = Await.ready(awaitable, timeout.duration)
    val futureValue = future.value.get
    futureValue match {
      case Failure(ex) if ex.getClass.equals(t.runtimeClass) => () //ok
      case x => fail(s"Unexpected result: $x")
    }
  }

  def expectReply[O](obj: O) {
    expectMsg(20.seconds, obj)
  }

}
