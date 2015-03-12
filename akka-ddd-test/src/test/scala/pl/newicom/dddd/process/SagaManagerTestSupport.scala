package pl.newicom.dddd.process

import akka.testkit.TestKit
import akka.util.Timeout
import eventstore._
import org.json4s.Formats
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.eventstore.EventMessageUnmarshaller

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait SagaManagerTestSupport extends EventMessageUnmarshaller {
  this: TestKit =>

  private val esExtension = EventStoreExtension(system)
  implicit val timeout = Timeout(5.seconds)

  def storeEvents(stream: String, events: EventMessage*)(implicit formats: Formats): Unit = {
    import akka.pattern.ask
    import org.json4s.native.Serialization.write

    def toEventData(em: EventMessage) = {
      val event = em.event
      val eventDoc = write(event)
      val metadataDoc = write(Map("id" -> em.id, "timestamp" -> em.timestamp))
      EventData(event.getClass.getName, data = Content(eventDoc), metadata = Content(metadataDoc))
    }

    Await.ready(esExtension.actor ? WriteEvents(EventStream.Id(stream), events.map(toEventData).toList), timeout.duration)
  }

}
