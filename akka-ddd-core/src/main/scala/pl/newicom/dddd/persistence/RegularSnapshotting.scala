package pl.newicom.dddd.persistence

import akka.actor.Actor.Receive
import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.Inner
import akka.persistence.{SaveSnapshotSuccess, PersistentActor}

case class RegularSnapshottingConfig(interest: Receive, interval: Int)

trait RegularSnapshotting {
  this: PersistentActor with ReceivePipeline =>

  def snapshottingConfig: RegularSnapshottingConfig

  private var receivedSinceLastSnapshot: Int = 0
  private var saveSnapshotRequestInProcess: Boolean = false

  private def snapshottingInterval: Int =
    snapshottingConfig.interval

  private def isTimeForSnapshot(extraInterval: Int = 0): Boolean =
    (receivedSinceLastSnapshot - extraInterval) >= snapshottingInterval

  pipelineOuter {
    case sss @ SaveSnapshotSuccess(_) =>
      saveSnapshotRequestInProcess = false
      receivedSinceLastSnapshot = 0
      Inner(sss)

    case msg if isMessageCounted(msg) =>
      val extraInterval = if (saveSnapshotRequestInProcess) snapshottingInterval else 0
      if (isTimeForSnapshot(extraInterval)) {
        self ! SaveSnapshotRequest
        saveSnapshotRequestInProcess = true
      }
      receivedSinceLastSnapshot += 1
      Inner(msg)
  }

  def isMessageCounted(msg: Any): Boolean = {
    !msg.isInstanceOf[SaveSnapshotRequest.type] && snapshottingConfig.interest.isDefinedAt(msg)
  }
}
