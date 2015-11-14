package pl.newicom.dddd.test.support

import akka.actor._
import akka.testkit.{TestKitBase, TestProbe}
import akka.util.Timeout
import org.scalatest.Suite

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
