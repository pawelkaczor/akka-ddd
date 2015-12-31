package pl.newicom.eventstore

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import eventstore._
import eventstore.pipeline.TickGenerator.{Tick, Trigger}
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.{EventMessageEntry, EventStreamSubscriber}
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.InFlightMessagesCallback

class DemandController(triggerActor: ActorRef, bufferSize: Int, initialDemand: Int = 20) extends InFlightMessagesCallback {

  increaseDemand(initialDemand)

  def onChanged(messagesInFlight: Int): Unit = {
    increaseDemand(bufferSize - messagesInFlight)
  }

  private def increaseDemand(increaseValue: Int): Unit =
    for (i <- 1 to increaseValue)
      triggerActor ! Tick(null)
}

trait EventstoreSubscriber extends EventStreamSubscriber with EventSourceProvider {
  this: Actor =>

  override def system = context.system

  def bufferSize: Int = 20

  implicit val actorMaterializer = ActorMaterializer()

  def subscribe(observable: BusinessEntity, fromPosExcl: Option[Long]): InFlightMessagesCallback = {

   def flow: Flow[Trigger, EventMessageEntry, Unit] = Flow() { implicit b =>
      import FlowGraph.Implicits._
      val zip = b.add(ZipWith((msg: EventMessageEntry, trigger: Trigger) => msg))

      eventSource(EsConnection(system), observable, fromPosExcl) ~> zip.in0
      (zip.in1, zip.out)
    }

    val sink = Sink.actorRef(self, onCompleteMessage = Kill)
    val triggerSource = Source.actorRef(bufferSize, OverflowStrategy.dropNew)

    val triggerActor = flow.toMat(sink)(Keep.both).runWith(triggerSource)

    new DemandController(triggerActor, bufferSize)
  }

}
