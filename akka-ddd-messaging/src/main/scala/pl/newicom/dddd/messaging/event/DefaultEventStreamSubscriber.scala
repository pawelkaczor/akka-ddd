package pl.newicom.dddd.messaging.event

import akka.NotUsed
import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import pl.newicom.dddd.BusinessEntity
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.{DemandCallback, DemandConfig}

trait Trigger
case object Tick extends Trigger

class DemandController(triggerActor: ActorRef, initialDemand: Int) extends DemandCallback {

  increaseDemand(initialDemand)

  override def onEventProcessed(): Unit = {
    increaseDemand(1)
  }

  private def increaseDemand(increaseValue: Int): Unit =
    for (i <- 1 to increaseValue)
      triggerActor ! Tick
}

trait DefaultEventStreamSubscriber extends EventStreamSubscriber {
  this: Actor with EventSourceProvider =>

  implicit val actorMaterializer = ActorMaterializer()

  override def subscribe(observable: BusinessEntity, fromPosExcl: Option[Long], demandConfig: DemandConfig): DemandCallback = {
    subscribe(eventSource(eventStore, observable, fromPosExcl), demandConfig)
  }

  private def subscribe(eventSource: EventSource, demandConfig: DemandConfig): DemandCallback = {

    def flow: Flow[Trigger, EventMessageEntry, NotUsed] = Flow.fromGraph(
      GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        val zip = b.add(ZipWith(Keep.left[EventMessageEntry, Trigger]))

        eventSource ~> zip.in0
        FlowShape(zip.in1, zip.out)
      })

    val triggerActor = flow
      .toMat {
        Sink.actorRef(self, onCompleteMessage = Kill)}(Keep.both)
      .runWith {
        Source.actorRef(demandConfig.subscriberCapacity, OverflowStrategy.dropNew)
      }

    new DemandController(triggerActor, demandConfig.initialDemand)
  }

}