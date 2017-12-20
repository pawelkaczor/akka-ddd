package pl.newicom.dddd.test.pm

import akka.actor._
import akka.testkit.TestKit
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpecLike}
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import pl.newicom.dddd.actor.ActorFactory
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.MetaAttribute.Correlation_Id
import pl.newicom.dddd.messaging.MetaData
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.office.{LocalOfficeId, OfficeListener, OfficeRef}
import pl.newicom.dddd.process.{Saga, SagaActorFactory}
import pl.newicom.dddd.test.ar.ARSpec.sys
import pl.newicom.dddd.utils.UUIDSupport._

import scala.concurrent.duration._
import scala.reflect.ClassTag


/**
 * @param sharePM if set to true, the same PM instance will be used in all tests, default is false
 */
abstract class PMSpec[PM <: Saga : SagaActorFactory : LocalOfficeId](_system: Option[ActorSystem] = None, val sharePM: Boolean = false)(implicit pmClassTag: ClassTag[PM])
  extends GivenWhenThenPMTestFixture(_system.getOrElse(sys(pmClassTag.runtimeClass))) with WordSpecLike with BeforeAndAfterAll with BeforeAndAfter {

  val logger: Logger = getLogger(getClass)

  override def officeUnderTest: OfficeRef = {
    implicit val _ = new OfficeListener[PM]
    if (_officeUnderTest == null) _officeUnderTest = office[PM]
    _officeUnderTest
  }

  private var _officeUnderTest: OfficeRef = _

  implicit var _pmIdGen: Gen[EntityId] = _

  val testSuiteId: String = uuid10

  before {
    _pmIdGen = Gen.const[String](if (sharePM) testSuiteId else uuid10)
  }

  after {
    ensureOfficeTerminated() //will nullify _officeUnderTest
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  def pmId(implicit pmIdGen: Gen[EntityId]): EntityId = pmIdGen.sample.get

  override def eventMetaDataProvider(e: DomainEvent): MetaData =
    MetaData(Correlation_Id -> pmId)

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
