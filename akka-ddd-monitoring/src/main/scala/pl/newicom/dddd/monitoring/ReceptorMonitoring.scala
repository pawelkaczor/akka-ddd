package pl.newicom.dddd.monitoring

import akka.NotUsed
import akka.stream.scaladsl.Source
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.{EventMessageEntry, EventSourceProvider}

trait ReceptorMonitoring[ES] extends EventSourceProvider[ES] with TraceContextSupport {

  override abstract def eventSource(eventStore: ES, observable: BusinessEntity, fromPositionExclusive: Option[Long]): Source[EventMessageEntry, NotUsed] = {
    super.eventSource(eventStore, observable, fromPositionExclusive).map {
      case entry @ EventMessageEntry(em, _) =>
        setNewCurrentTraceContext(s"${observable.id}-${em.payloadName}")
        entry
    }
  }

}
