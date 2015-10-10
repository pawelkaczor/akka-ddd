package pl.newicom.dddd.view.sql

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import com.typesafe.config.Config
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.messaging.event.DomainEventMessage
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{CreateDummy, DummyCreated}
import pl.newicom.dddd.test.dummy.{DummyAggregateRoot, _}
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.view.sql.SqlViewUpdateServiceIntegrationSpec._

import scala.concurrent.duration._
import scala.slick.jdbc.JdbcBackend

object SqlViewUpdateServiceIntegrationSpec {
  implicit val sys: ActorSystem = ActorSystem("SqlViewUpdateServiceSpec")

  implicit def dummyFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it
    }

  case class ViewUpdated(event: AnyRef)
}

class SqlViewUpdateServiceIntegrationSpec extends OfficeSpec[DummyAggregateRoot] with SqlViewStoreTestSupport{

  "SqlViewUpdateService" should {
    "propagate events from event store to configured projection" in {
      // Given
      val probe = TestProbe()
      probe.ignoreMsg {
        case ViewUpdated(DummyCreated(id, _, _, _)) => !id.equals(aggregateId)
        case ViewUpdated(_) => true
      }
      system.eventStream.subscribe(probe.ref, classOf[ViewUpdated])

      system.actorOf(Props(
        new SqlViewUpdateService with SqlViewStoreConfiguration {
          def config = SqlViewUpdateServiceIntegrationSpec.this.config
          def configuration = List(SqlViewUpdateConfig("test-view", dummyOffice, new Projection {
            def consume(em: DomainEventMessage)(implicit s: JdbcBackend.Session): Unit = {
              system.eventStream.publish(ViewUpdated(em.event))
            }
          }))
        }
      ))

      // When
      when {
        CreateDummy(aggregateId, "name", "description", 100)
      }
      // Then
      .expect { c =>
        DummyCreated(c.id, c.name, c.description, c.value)
      }
      probe.expectMsg(ViewUpdated(DummyCreated(aggregateId, "name", "description", 100)))

    }
  }

  lazy val viewMetadataDao = new ViewMetadataDao()

  override def dropSchema(implicit s: JdbcBackend.Session): Unit = {
    viewMetadataDao.dropSchema
  }

  override def createSchema(implicit s: JdbcBackend.Session): Unit = {
    viewMetadataDao.createSchema
  }

  override def config: Config = system.settings.config
}
