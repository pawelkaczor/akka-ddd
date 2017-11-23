package pl.newicom.dddd.test.support

import akka.actor._
import akka.testkit.TestKit
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpecLike}
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import pl.newicom.dddd.actor.{ActorFactory, BusinessEntityActorFactory}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.office.{LocalOfficeId, OfficeListener, OfficeRef}
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.test.support.OfficeSpec.sys
import pl.newicom.dddd.utils.UUIDSupport._

import scala.concurrent.duration._
import scala.reflect.ClassTag

object OfficeSpec {
  def sys(arClass: Class[_]) = ActorSystem(s"${arClass.getSimpleName}OfficeSpec_$uuid7")
}

/**
 * @param shareAggregateRoot if set to true, the same AR instance will be used in all tests, default is false
 */
abstract class OfficeSpec[Event <: DomainEvent, AR <: AggregateRoot[Event, _, AR] : BusinessEntityActorFactory : LocalOfficeId](_system: Option[ActorSystem] = None, val shareAggregateRoot: Boolean = false)(implicit arClassTag: ClassTag[AR])
  extends GivenWhenThenTestFixture[Event](_system.getOrElse(sys(arClassTag.runtimeClass))) with WithGenOfficeSpec with WordSpecLike with BeforeAndAfterAll with BeforeAndAfter {

  val logger: Logger = getLogger(getClass)

  override def officeUnderTest: OfficeRef = {
    implicit val _ = new OfficeListener[AR]
    if (_officeUnderTest == null) _officeUnderTest = office[AR]
    _officeUnderTest
  }

  private var _officeUnderTest: OfficeRef = _

  implicit var _aggregateIdGen: Gen[AggregateId] = _

  implicit def aggregatedIdArb: Arbitrary[AggregateId] = Arbitrary(_aggregateIdGen)

  val testSuiteId: String = uuid10

  before {
    _aggregateIdGen = Gen.const[String](if (shareAggregateRoot) testSuiteId else uuid10).map(s => new AggregateId(s))
  }

  after {
    ensureOfficeTerminated() //will nullify _officeUnderTest
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  def aggregateId(implicit aggregateIdGen: Gen[AggregateId]): AggregateId = aggregateIdGen.sample.get

  implicit def topLevelParent[T : LocalOfficeId](implicit system: ActorSystem): ActorFactory[T] = {
    new ActorFactory[T] {
      override def getChild(name: String): Option[ActorRef] = None
      override def createChild(props: Props, name: String): ActorRef = {
        system.actorOf(props, name)
      }
    }
  }

  def ensureTerminated(actor: ActorRef): Any = {
    watch(actor) ! PoisonPill
    fishForMessage(1.seconds) {
      case Terminated(_) =>
        unwatch(actor)
        true
      case _ => false
    }
  }

  override def ensureOfficeTerminated(): Unit = {
    if (_officeUnderTest != null) {
      ensureTerminated(_officeUnderTest.actor)
    }
    _officeUnderTest = null
  }

}
