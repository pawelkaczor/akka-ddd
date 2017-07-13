package pl.newicom.dddd.test.dms

import org.joda.time.DateTime
import org.joda.time.DateTime.now
import pl.newicom.dddd.aggregate.error.DomainException
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.test.dms.DocumentAR.DMSActionsRoot
import pl.newicom.dddd.test.dms.DMSProtocol.VersionUpdate.creation
import pl.newicom.dddd.test.dms.DMSProtocol._

import scala.collection.immutable.SortedMap

object DocumentAR extends AggregateRootSupport {

  implicit val initialVersions: Uninitialized[DMSActionsRoot] = Gateway(SortedMap())

  sealed trait DMSActionsRoot extends AggregateActions[DMSEvent, DMSActionsRoot, Config]

  abstract class Document extends DMSActionsRoot {
    def version: Version
    val epoch: DateTime = now
  }

  //
  // Gateway
  //

  case class Gateway(docsByVersion: SortedMap[Version, Document]) extends DMSActionsRoot with Uninitialized[DMSActionsRoot] {

    def actions: Actions =
      handleCommand {
        case c: TargetingVersion =>
          rejectIf(!docsByVersion.contains(c.version), s"Unknown version: ${c.version}").orElse {
            docsByVersion(c.version).commandHandlerNoCtx(c)
          }

        case c: DMSCommand =>
          val ch = latestOrNew.commandHandlerNoCtx
          if (ch.isDefinedAt(c)) {
            ch(c)
          } else {
            reject(new DomainException(s"Document does not exist. $c can not be processed: missing command handler!"))
          }
      }.handleEvent {
          case e: DMSEvent =>
            if (docsByVersion.isEmpty && e.versionUpdate.isCreation)
              UntitledDocument.apply(e)
            else
              docsByVersion(e.versionUpdate.from.get).apply(e)
      }
      .map(withDocument)
      .handleQuery[GetPublishedVersions] { q =>
          reply(PublishedVersions(docsByVersion.keySet))
      }
      .handleQuery[GetPublishedRevisions] { q =>
        reply(PublishedRevisions(docsByVersion.map {
          case (a, b) => Revision("a", a, b.epoch)
        }.toSet))
      }

    private def withDocument(doc: Document): Gateway =
      copy(docsByVersion + (doc.version -> doc))

    private def latestOrNew: Document =
      if (docsByVersion.isEmpty) UntitledDocument else latest

    private def latest: Document =
      docsByVersion.last._2
  }

  //
  // Versioning
  //

  case class Versioned(version: Version) extends DMSActionsRoot {

    def actions =
      handleCommand {
        case c @ Publish(docId, _, _) =>
          Published(docId, vu(c))
      }.handleEvent {
        case Published(_, vu) =>
          copy(vu.to)
      }

    def vu(c: CreatesNewVersion) = c.majorUpdate match {
      case Some(true)  => VersionUpdate(Some(version), version.bumpMajor)
      case Some(false) => VersionUpdate(Some(version), version.bumpMinor)
      case None        => VersionUpdate(Some(version), version)
    }

    def noVU = VersionUpdate.noVU(version)

  }

  //
  // Document Actions
  //

  case object UntitledDocument extends Document {
    val version = Version(0, 0)

    def actions: Actions =
      handleCommand {
        case Create(docId, title, content) =>
          Created(docId, title, content, creation)
      }.handleEvent {
        case Created(_, title, _, vu) =>
          TitledDocument(title, Versioned(vu.to))
      }
  }

  case class TitledDocument(title: String, versioned: Versioned) extends Document {
    def version = versioned.version

    def actions = editing.orElse(versioned, (v: Versioned) => copy(versioned = v))

    def editing =
      handleCommand {
        case c @ ChangeContent(docId, _, content) =>
          ContentChanged(docId, content, versioned.noVU)
      }.handleEvent {
        case ContentChanged(_, _, vu) =>
          copy(versioned = versioned.copy(version = vu.to))
      }
  }

}

class DocumentAR(val config: Config) extends AggregateRoot[DMSEvent, DMSActionsRoot, DocumentAR]
  with ConfigClass[Config]
  with AggregateRootLogger[DMSEvent]