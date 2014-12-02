package pl.newicom.dddd.test

import java.util.UUID

import akka.actor._
import akka.testkit.{EventFilter, ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.messaging.correlation.AggregateIdResolution

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.util.Failure

abstract class EventsourcedAggregateRootSpec[A](_system: ActorSystem)(implicit arClassTag: ClassTag[A])
  extends TestKit(_system)
  with ImplicitSender
  with WordSpecLike with Matchers
  with BeforeAndAfterAll with BeforeAndAfter {

  val logger = LoggerFactory.getLogger(getClass)

  val domain = arClassTag.runtimeClass.getSimpleName

  implicit val aggregateIdGen: Gen[EntityId] = Gen.const[EntityId](domain + "-" + UUID.randomUUID().toString.subSequence(0, 6))

  case class When(when: () => Unit) {
    def expectEventPublishedMatching[E](matcher: PartialFunction[Any, E])(implicit t: ClassTag[E]): E = {
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, t.runtimeClass)
      when()
      probe.expectMsgPF[E](10 seconds)(matcher)
    }
  }

  def when(whenFun: => Unit) = When(() => whenFun)

  def aggregateId(implicit aggregateIdGen: Gen[EntityId]): EntityId = aggregateIdGen.sample.get

  def arbitraryForCurrentAR[T](implicit aggregateIdGen: Gen[EntityId], a: Arbitrary[T]): T = {
    a.arbitrary.sample.get
  }

  implicit def topLevelParent(implicit system: ActorSystem): CreationSupport = {
    new CreationSupport {
      override def getChild(name: String): Option[ActorRef] = None
      override def createChild(props: Props, name: String): ActorRef = {
        system.actorOf(props, name)
      }
    }
  }

  implicit def defaultCaseIdResolution[AA]: AggregateIdResolution[AA] = new AggregateIdResolution[AA]

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    system.awaitTermination()
  }

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

  def ensureActorTerminated(actor: ActorRef) = {
    watch(actor)
    actor ! PoisonPill
    // wait until reservation office is terminated
    fishForMessage(1.seconds) {
      case Terminated(_) =>
        unwatch(actor)
        true
      case _ => false
    }

  }


}
