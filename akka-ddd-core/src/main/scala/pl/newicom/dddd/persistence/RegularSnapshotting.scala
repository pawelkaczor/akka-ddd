package pl.newicom.dddd.persistence

import akka.actor.Actor.Receive
import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.Inner
import akka.persistence.PersistentActor

case class RegularSnapshottingConfig(interest: Receive, interval: Int)

trait RegularSnapshotting {
  this: PersistentActor with ReceivePipeline =>

  def snapshottingConfig: RegularSnapshottingConfig

  private var receivedSinceLastSnapshot: Int = 0

  pipelineInner {
    case msg ⇒
      if (snapshottingConfig.interest.isDefinedAt(msg)) {
        if (receivedSinceLastSnapshot >= snapshottingConfig.interval) {
          receivedSinceLastSnapshot = 0
          self ! SaveSnapshotRequest
        } else {
          receivedSinceLastSnapshot += 1
        }
      }
      Inner(msg)
  }
}
