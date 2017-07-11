package pl.newicom.dddd.messaging.event

import akka.NotUsed
import akka.actor.{Actor, ActorSystem}
import akka.event.LoggingAdapter
import akka.persistence.query.{Offset, PersistenceQuery}
import akka.persistence.query.scaladsl.EventsByTagQuery
import akka.stream.scaladsl.Source
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.office.CaseRef

trait EventSourceProvider extends EventStoreProvider {
  this: Actor =>

  def system: ActorSystem = context.system

  type EventSource = Source[EventMessageEntry, NotUsed]
  type ReadJournal <: EventsByTagQuery

  def log: LoggingAdapter
  def readJournalId: String

  lazy val readJournal: ReadJournal = PersistenceQuery(system).readJournalFor(readJournalId)

  def eventSource(eventStore: EventStore, observable: BusinessEntity, fromPosExcl: Option[Long]): EventSource = {

    log.debug(s"Subscribing to ${observable.id} from position $fromPosExcl (exclusive)")

    val offset = fromPosExcl.map(Offset.sequence).getOrElse(Offset.noOffset)

    readJournal.eventsByTag(observable.id, offset).map { envelop =>

      envelop.event match {

        case em: EventMessage =>
          val caseRef = CaseRef(envelop.persistenceId, observable, Some(envelop.sequenceNr))
          val oem = OfficeEventMessage(em, caseRef).withEventNumber(envelop.sequenceNr.toInt)
          EventMessageEntry(oem, envelop.sequenceNr, Some(em.timestamp))

        case unexpected =>
          throw new RuntimeException(s"Unexpected msg received: $unexpected")

      }
    }
  }

}
