package pl.newicom.eventstore

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import eventstore.EventNumber._
import eventstore._
import eventstore.pipeline.TickGenerator.{Tick, Trigger}
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.{InFlightMessagesCallback, EventReceived}
import pl.newicom.dddd.messaging.event.{EventStream, _}

class DemandController(triggerActor: ActorRef, bufferSize: Int, initialDemand: Int = 1) extends InFlightMessagesCallback {

  increaseDemand(initialDemand)

  def onChanged(messagesInFlight: Int): Unit = {
    increaseDemand(bufferSize - messagesInFlight)
  }

  private def increaseDemand(increaseValue: Int): Unit =
    for (i <- 1 to increaseValue)
      triggerActor ! Tick(null)
}

trait EventstoreSubscriber extends EventStreamSubscriber with EventstoreSerializationSupport with ActorLogging {
  this: Actor =>

  override def system = context.system

  def bufferSize: Int = 10

  implicit val actorMaterializer = ActorMaterializer()

  def subscribe(stream: EventStream, fromPosExcl: Option[Long]): InFlightMessagesCallback = {

    def eventSource: Source[EventReceived, Unit] = {
      def withMetaData(eventData: EventData): EventMessage = {
        val em = toEventMessage(eventData).get
        em.withMetaData(metaDataProvider(em))
      }
      val streamId = StreamNameResolver.streamId(stream)
      log.debug(s"Subscribing to $streamId from position $fromPosExcl (exclusive)")
      Source(
        EsConnection(system).streamPublisher(
          streamId,
          fromPosExcl.map(l => Exact(l.toInt)),
          resolveLinkTos = true
        )
      ).map {
        case EventRecord(_, number, eventData, _) =>
          EventReceived(withMetaData(eventData), number.value)
        case ResolvedEvent(EventRecord(_, _, eventData, _), linkEvent) =>
          EventReceived(withMetaData(eventData), linkEvent.number.value)
      }
    }

   def flow: Flow[Trigger, EventReceived, Unit] = Flow() { implicit b =>
      import FlowGraph.Implicits._
      val zip = b.add(ZipWith((msg: EventReceived, trigger: Trigger) => msg))

      eventSource ~> zip.in0
      (zip.in1, zip.out)
    }

    val sink = Sink.actorRef(self, onCompleteMessage = Kill)
    val triggerSource = Source.actorRef(bufferSize, OverflowStrategy.dropNew)

    val triggerActor = flow.toMat(sink)(Keep.both).runWith(triggerSource)

    new DemandController(triggerActor, bufferSize)
  }

}
