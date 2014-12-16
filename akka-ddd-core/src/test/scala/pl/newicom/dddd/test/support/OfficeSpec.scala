package pl.newicom.dddd.test.support

import java.util.UUID.randomUUID

import akka.actor._
import akka.testkit.TestKit
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpecLike}
import org.slf4j.LoggerFactory.getLogger
import pl.newicom.dddd.actor.{BusinessEntityActorFactory, CreationSupport}
import pl.newicom.dddd.aggregate.{BusinessEntity, EntityId}
import pl.newicom.dddd.messaging.correlation.AggregateIdResolution
import pl.newicom.dddd.office.LocalOffice._
import pl.newicom.dddd.office.Office._

import scala.concurrent.duration._
import scala.reflect.ClassTag

abstract class OfficeSpec[A <: BusinessEntity : BusinessEntityActorFactory](_system: ActorSystem)(implicit arClassTag: ClassTag[A])
  extends GivenWhenThenTestFixture(_system) with WordSpecLike with BeforeAndAfterAll with BeforeAndAfter {

  val logger = getLogger(getClass)

  val domain = arClassTag.runtimeClass.getSimpleName

  override def officeUnderTest: ActorRef = {
    if (_officeUnderTest == null) _officeUnderTest = office[A]
    _officeUnderTest
  }

  private var _officeUnderTest: ActorRef = null

  implicit var aggregateIdGen: Gen[EntityId] = null

  before {
    aggregateIdGen = Gen.const[EntityId](domain + "-" + randomUUID().toString.subSequence(0, 6))
  }

  after {
    ensureOfficeTerminated() //will nullify _officeUnderTest
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    system.awaitTermination()
  }

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

  def ensureOfficeTerminated(): Unit = {
    ensureActorTerminated(_officeUnderTest)
    _officeUnderTest = null
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
