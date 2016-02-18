package pl.newicom.dddd.view.sql

import akka.actor.Props
import akka.event.EventStream
import akka.testkit.TestProbe
import com.typesafe.config.Config
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.messaging.event.OfficeEventMessage
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{DummyEvent, CreateDummy, DummyCreated}
import pl.newicom.dddd.test.dummy.{DummyAggregateRoot, _}
import pl.newicom.dddd.test.support.IntegrationTestConfig.integrationTestSystem
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.view.sql.Projection.ProjectionAction
import pl.newicom.dddd.view.sql.SqlViewUpdateServiceIntegrationSpec._
import pl.newicom.eventstore.EventSourceProvider
import slick.backend.DatabaseComponent
import slick.dbio._
import slick.dbio.DBIOAction.{failed, successful}
import slick.dbio.Effect.All
import slick.util.DumpInfo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SqlViewUpdateServiceIntegrationSpec {

  implicit def dummyFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it
    }

  case class ViewUpdated(event: DummyEvent)

  case class PublishAction(eventStream: EventStream, event: ViewUpdated) extends SynchronousDatabaseAction[Unit, NoStream, DatabaseComponent, Effect] {
    def getDumpInfo = DumpInfo("success", "unit")
    def run(ctx: DatabaseComponent#Context): Unit = {
      eventStream.publish(event)
    }
  }
}

/**
 * Requires Event Store (with projections enabled!) to be up and running.
 */
class SqlViewUpdateServiceIntegrationSpec
  extends OfficeSpec[DummyAggregateRoot](Some(integrationTestSystem("SqlViewUpdateServiceIntegrationSpec")))
  with SqlViewStoreTestSupport {

  "SqlViewUpdateService" should {
    "propagate events from event store to configured projection" in {
      // Given
      var shouldFail = false
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[ViewUpdated])

      system.actorOf(Props(
        new SqlViewUpdateService with SqlViewStoreConfiguration with EventSourceProvider {
          def config = SqlViewUpdateServiceIntegrationSpec.this.config
          def vuConfigs = List(SqlViewUpdateConfig("test-view", dummyOfficeId, new Projection {

            def failIfRequired(msg: String) =
              if (shouldFail) failed(new RuntimeException(msg)) else successful(())

            def consume(em: OfficeEventMessage): ProjectionAction[All] = {
              val event = em.event.asInstanceOf[DummyEvent]
              val ignore = !aggregateId.equals(event.id)
              if (ignore)
                successful(())
              else {
                shouldFail = !shouldFail
                failIfRequired("Projection failed (test)") >>
                    PublishAction(system.eventStream, ViewUpdated(event))
              }
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

  override def ensureSchemaDropped = viewMetadataDao.ensureSchemaDropped

  override def ensureSchemaCreated = viewMetadataDao.ensureSchemaCreated


  override def config: Config = system.settings.config

}
