package pl.newicom.dddd.test.dms

import akka.actor.Props
import pl.newicom.dddd.aggregate.{AggregateRootActorFactory, DefaultConfig}
import pl.newicom.dddd.office.OfficeRef
import pl.newicom.dddd.test.dms.DMSProtocol.VersionUpdate.{creation, noVU}
import pl.newicom.dddd.test.dms.DMSProtocol.{ChangeContent, ContentChanged, Create, Created, DMSEvent, DocId, GetPublishedRevisions, GetPublishedVersions, Publish, Published, PublishedRevisions, PublishedVersions, Revision, Version, VersionUpdate}
import pl.newicom.dddd.test.support.TestConfig.testSystem
import DMSSpec._
import akka.util.Timeout
import org.scalatest.Matchers
import pl.newicom.dddd.aggregate.error.DomainException
import pl.newicom.dddd.test.ar.ARSpec

import scala.concurrent.Await

object DMSSpec {
  implicit def actorFactory: AggregateRootActorFactory[DocumentAR] =
    AggregateRootActorFactory[DocumentAR](pc => Props(new DocumentAR(DefaultConfig(pc))))

  def v(major: Int, minor: Int) = Version(major, minor)
  def vu(from: Version, to: Version) = VersionUpdate(Some(from), to)
}

class DMSSpec extends ARSpec[DMSEvent, DocumentAR](Some(testSystem), shareAggregateRoot = true) with Matchers {

  def cmsOffice: OfficeRef = officeUnderTest

  def docId: DocId = aggregateId
  implicit def toRevision(version: Version): Revision = Revision(docId, version)

  "CMS office" should {

    "create v0.1" in {
      when {
        Create(docId, "title", "content 0.1")
      }
      .expect { c =>
        Created(docId, c.title, c.content, creation)
      }
    }

    "change content of v0.1 (prepare 0.2)" in {
      when {
        ChangeContent(docId, v(0, 1), "content 0.2")
      }
      .expect { c =>
        ContentChanged(docId, c.content, noVU(v(0, 1)))
      }
    }

    "publish (v0.1 -> v0.2)" in {
      when {
        Publish(docId, v(0, 1), majorUpdate = Some(false))
      }
      .expect { c =>
        Published(docId, vu(v(0, 1), v(0, 2)))
      }
    }

    "change content of v0.2" in {
      when {
        ChangeContent(docId, v(0, 2), "content 0.2 with some changes")
      }
      .expect { c =>
        ContentChanged(docId, c.content, noVU(v(0, 2)))
      }
    }

    "publish (v0.2 -> v1.0)" in {
      when {
        Publish(docId, v(0, 2), majorUpdate = Some(true))
      }
      .expect { c =>
        Published(docId, vu(v(0, 2), v(1, 0)))
      }
    }

    "change content of v1.0" in {
      when {
        ChangeContent(docId, v(0, 2), "content 1.0")
      }
      .expect { c =>
        ContentChanged(docId, c.content, noVU(v(1, 0)))
      }
    }

    "publish (v0.2 -> v0.3)" in {
      when {
        Publish(docId, v(0, 2), majorUpdate = Some(false))
      }
      .expect { c =>
        Published(docId, vu(v(0, 2), v(0, 3)))
      }
    }

    "reject publishing unknown version (v0.4 -> v0.5)" in {
      when {
        Publish(docId, v(0, 4), majorUpdate = Some(false))
      }
      .expectException[DomainException]("Unknown version: v0.4")
    }

    "respond to GetPublishedVersions query" in {
      import system.dispatcher
      implicit val t: Timeout = timeout

      val response: PublishedVersions =
        Await.result(cmsOffice ? GetPublishedVersions(docId), t.duration)

      response.versions should contain (v(0, 1))
      response.versions should contain (v(0, 2))
      response.versions should contain (v(0, 3))
      response.versions should contain (v(1, 0))
    }

    "respond to GetPublishedRevisions query" in {
      import system.dispatcher
      implicit val t: Timeout = timeout

      val response: PublishedRevisions =
        Await.result(cmsOffice ? GetPublishedRevisions(docId), t.duration)

      response.revisions.map(_.version) should contain (v(0, 1))
    }

  }
}