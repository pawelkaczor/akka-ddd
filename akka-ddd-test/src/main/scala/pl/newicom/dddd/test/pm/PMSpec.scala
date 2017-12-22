package pl.newicom.dddd.test.pm

import akka.actor._
import akka.testkit.TestKit
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils.{currentTimeMillis, setCurrentMillisFixed}
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
import pl.newicom.dddd.saga.ProcessConfig
import pl.newicom.dddd.test.ar.ARSpec.sys
import pl.newicom.dddd.utils.UUIDSupport

import scala.concurrent.duration._
import scala.reflect.ClassTag


abstract class PMSpec[PM <: Saga : SagaActorFactory : LocalOfficeId : ProcessConfig](_system: Option[ActorSystem] = None)(implicit pmClassTag: ClassTag[PM])
  extends GivenWhenThenPMTestFixture(_system.getOrElse(sys(pmClassTag.runtimeClass))) with WordSpecLike with BeforeAndAfterAll with BeforeAndAfter with UUIDSupport {

  val logger: Logger = getLogger(getClass)

  val testEpoch: DateTime = new DateTime(currentTimeMillis())
  setCurrentMillisFixed(testEpoch.getMillis)

  override def officeUnderTest: OfficeRef = {
    implicit val _ = new OfficeListener[PM]
    if (_officeUnderTest == null) _officeUnderTest = office[PM]
    _officeUnderTest
  }

  private var _officeUnderTest: OfficeRef = _

  private var _testId: EntityId = _
  val testSuiteId: EntityId = uuid10

  before {
    _testId = uuid10
  }

  after {
    ensureOfficeTerminated() //will nullify _officeUnderTest
  }

  def testId: EntityId = _testId

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  override def eventMetaDataProvider(e: DomainEvent): MetaData =
    MetaData(Correlation_Id -> implicitly[ProcessConfig[PM]].correlationIdResolver(e))

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
