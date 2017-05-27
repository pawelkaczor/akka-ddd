package pl.newicom.dddd.test.dms

import org.joda.time.DateTime
import org.joda.time.DateTime.now
import pl.newicom.dddd.aggregate
import pl.newicom.dddd.aggregate.{EntityId, Query}

import scala.math.Ordering

object DMSProtocol {

  //
  // Abstract Commands
  //

  sealed trait DMSCommand extends aggregate.Command {
    def docId: EntityId
    override def aggregateId: String = docId
  }

  trait TargetingVersion { this: DMSCommand =>
    def docId: EntityId
    def version: Version
  }

  trait CreatesNewVersion extends TargetingVersion { this: DMSCommand =>
    // if not defined, AR will have to decide
    def majorUpdate: Option[Boolean]
  }

  trait CreatesNewMajorVersion extends CreatesNewVersion { this: DMSCommand =>
    override def majorUpdate: Option[Boolean] = Some(true)
  }

  trait CreatesNewMinorVersion extends CreatesNewVersion with TargetingVersion { this: DMSCommand =>
    override def majorUpdate: Option[Boolean] = Some(false)
  }

  //
  // Commands
  //

  case class Create(docId: EntityId, title: String, content: String)                  extends DMSCommand
  case class Publish(docId: EntityId, version: Version, majorUpdate: Option[Boolean]) extends CreatesNewVersion with DMSCommand
  case class ChangeContent(docId: EntityId, version: Version, content: String)        extends DMSCommand

  //
  // Events
  //

  sealed trait DMSEvent {
    def docId: EntityId
    def versionUpdate: VersionUpdate
    def targetRevision: Revision = Revision(docId, targetVersion)
    def targetVersion: Version   = versionUpdate.to
  }

  case class Created(docId: EntityId, title: String, content: String, versionUpdate: VersionUpdate) extends DMSEvent
  case class ContentChanged(docId: EntityId, content: String, versionUpdate: VersionUpdate)         extends DMSEvent
  case class Published(docId: EntityId, versionUpdate: VersionUpdate)                               extends DMSEvent

  //
  // Queries
  //

  case class GetPublishedVersions(docId: EntityId) extends Query {
    def aggregateId: String = docId
    type R = PublishedVersions
  }

  case class PublishedVersions(versions: Set[Version])

  case class GetPublishedRevisions(docId: EntityId) extends Query {
    def aggregateId: String = docId
    type R = PublishedRevisions
  }

  case class PublishedRevisions(revisions: Set[Revision])

  //
  // Value Objects
  //

  trait VersionOrdering extends Ordering[Version] {
    def compare(x: Version, y: Version): Int = x.major.compare(y.major) match {
      case 0 => x.minor.compare(y.minor)
      case r => r
    }
  }

  implicit object VersionOrd extends VersionOrdering

  case class Version(major: Int, minor: Int) {
    def bumpMajor: Version = copy(major + 1, 0)
    def bumpMinor: Version = copy(minor = minor + 1)
    override def toString  = s"v$major.$minor"
  }

  case class Revision(docId: EntityId, version: Version, epoch: DateTime = now) {
    def bumpMajor: Revision = copy(docId, version.bumpMajor, now)
    def bumpMinor: Revision = copy(docId, version.bumpMinor, now)
  }

  object VersionUpdate {
    def creation: VersionUpdate               = VersionUpdate(None, Version(0, 1))
    def noVU(version: Version): VersionUpdate = VersionUpdate(Some(version), version)
  }

  case class VersionUpdate(from: Option[Version], to: Version) {
    def version: Version       = to
    def isMajorUpdate: Boolean = if (isCreation) true else to.major > from.get.major
    def isMinorUpdate: Boolean = if (isCreation) false else !isMajorUpdate && to.minor > from.get.minor
    def isCreation: Boolean    = from.isEmpty

    def withBumpMinor: VersionUpdate = copy(Some(to), to.bumpMinor)
    def withBumpMajor: VersionUpdate = copy(Some(to), to.bumpMajor)
    def withNoVU: VersionUpdate      = copy(Some(to), to)
  }

}
