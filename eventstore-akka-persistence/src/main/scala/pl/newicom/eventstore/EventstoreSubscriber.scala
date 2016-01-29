package pl.newicom.eventstore

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{FlowShape, ActorMaterializer, OverflowStrategy}
import eventstore._
import eventstore.pipeline.TickGenerator.{Tick, Trigger}
import pl.newicom.dddd.messaging.event.EventStreamSubscriber.{DemandConfig, DemandCallback}

import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.{EventMessageEntry, EventStreamSubscriber}

class DemandController(triggerActor: ActorRef, initialDemand: Int) extends DemandCallback {

  increaseDemand(initialDemand)

  override def onEventProcessed(): Unit = {
    increaseDemand(1)
  }

  private def increaseDemand(increaseValue: Int): Unit =
    for (i <- 1 to increaseValue)
      triggerActor ! Tick(null)
}

trait EventstoreSubscriber extends EventStreamSubscriber with EventSourceProvider {
  this: Actor =>

  override def system = context.system

  implicit val actorMaterializer = ActorMaterializer()

  def subscribe(observable: BusinessEntity, fromPosExcl: Option[Long], demandConfig: DemandConfig): DemandCallback = {

    def flow: Flow[Trigger, EventMessageEntry, Unit] = Flow.fromGraph(
      GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        val zip = b.add(ZipWith(Keep.left[EventMessageEntry, Trigger]))

        eventSource(eventstoreConnection, observable, fromPosExcl) ~> zip.in0
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

  def eventstoreConnection: EsConnection = {
    EsConnection(system)
  }
}