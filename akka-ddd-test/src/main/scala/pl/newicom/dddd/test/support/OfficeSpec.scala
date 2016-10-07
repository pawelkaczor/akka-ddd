package pl.newicom.dddd.test.support

import akka.actor._
import akka.testkit.TestKit
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpecLike}
import org.slf4j.LoggerFactory.getLogger
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, CreationSupport}
import pl.newicom.dddd.aggregate.{BusinessEntity, Command, EntityId}
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.office.{LocalOfficeId, Office, OfficeListener}
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.test.support.OfficeSpec.sys
import pl.newicom.dddd.utils.UUIDSupport._

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.reflect.ClassTag

object OfficeSpec {
  def sys(arClass: Class[_]) = ActorSystem(s"${arClass.getSimpleName}OfficeSpec_$uuid7")
}

/**
 * @param shareAggregateRoot if set to true, the same AR instance will be used in all tests, default is false
 */
abstract class OfficeSpec[A <: BusinessEntity : BusinessEntityActorFactory: LocalOfficeId](_system: Option[ActorSystem] = None, val shareAggregateRoot: Boolean = false)(implicit arClassTag: ClassTag[A])
  extends GivenWhenThenTestFixture(_system.getOrElse(sys(arClassTag.runtimeClass))) with WordSpecLike with BeforeAndAfterAll with BeforeAndAfter {

  val logger = getLogger(getClass)

  val domain = arClassTag.runtimeClass.getSimpleName

  override def officeUnderTest: Office = {
    implicit val _ = new OfficeListener[A]
    if (_officeUnderTest == null) _officeUnderTest = office[A]
    _officeUnderTest
  }

  private var _officeUnderTest: Office = _

  implicit var _aggregateIdGen: Gen[EntityId] = _

  val testSuiteId = uuid10

  before {
    _aggregateIdGen = Gen.const[EntityId](domain + (if (shareAggregateRoot) testSuiteId else uuid10))
  }

  after {
    ensureOfficeTerminated() //will nullify _officeUnderTest
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  def aggregateId(implicit aggregateIdGen: Gen[EntityId]): EntityId = aggregateIdGen.sample.get

  @tailrec
  implicit final def arbitraryToSample[T](g: Gen[T]): T = {
    g.sample match {
      case Some(x) => x
      case _ => arbitraryToSample(g)
    }
  }

  def a[T](implicit g: Gen[T]): Gen[T] = g
  def a_list_of[T1 <: Command, T2 <: Command, T3 <: Command, T4 <: Command](implicit g1: Gen[T1], g2: Gen[T2], g3: Gen[T3], g4: Gen[T4]): List[Command] = List(g1, g2, g3, g4)
  def a_list_of[T1 <: Command, T2 <: Command, T3 <: Command](implicit g1: Gen[T1], g2: Gen[T2], g3: Gen[T3]): List[Command] = List(g1, g2, g3)
  def a_list_of[T1 <: Command, T2 <: Command](implicit g1: Gen[T1], g2: Gen[T2]): List[Command] = List(g1, g2)

  def arbitraryOf[T](adjust: (T) => T = {x: T => x})(implicit g: Gen[T]): T = adjust(g)

  private def arbitrarySample[T](implicit g: Gen[T]): T = arbitraryToSample(g)

  implicit def topLevelParent(implicit system: ActorSystem): CreationSupport = {
    new CreationSupport {
      override def getChild(name: String): Option[ActorRef] = None
      override def createChild(props: Props, name: String): ActorRef = {
        system.actorOf(props, name)
      }
    }
  }

  def ensureActorUnderTestTerminated(actor: ActorRef) = {
    watch(actor)
    actor ! PoisonPill
    fishForMessage(1.seconds) {
      case Terminated(_) =>
        unwatch(actor)
        true
      case _ => false
    }

  }

  override def ensureOfficeTerminated(): Unit = {
    if (_officeUnderTest != null) {
      ensureActorUnderTestTerminated(_officeUnderTest.actor)
    }
    _officeUnderTest = null
  }

}
