package pl.newicom.dddd.clustertest

import akka.cluster.Cluster
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.duration._
import scala.reflect.ClassTag

class SimpleClusterConfig(config: Config) extends MultiNodeConfig {

  val node1 = role("node1")
  val node2 = role("node2")

  commonConfig(config)
}

abstract class SimpleClusterSpec(config: Config)
  extends MultiNodeSpec(new SimpleClusterConfig(config.withFallback(ConfigFactory.load("simple-cluster"))))
  with STMultiNodeSpec  with ImplicitSender {

  implicit val logger = system.log

  def firstNode: RoleName = roles.head
  def secondNode: RoleName = roles(1)

  def initialParticipants = roles.size

  def join(startOn: RoleName, joinTo: RoleName) {
    on(startOn) {
      Cluster(system) join node(joinTo).address
    }
    enterBarrier(startOn.name + "-joined")
  }

  def joinCluster() {
    join(startOn = roles.head, joinTo = roles.head)
    join(startOn = roles(1), joinTo = roles.head)
    enterBarrier("after-2")
  }

  def on(nodes: RoleName*)(thunk: ⇒ Unit): Unit = {
    runOn(nodes: _*)(thunk)
  }

  def expectReply[T](obj: T) {
    expectMsg(20.seconds, obj)
  }

  def expectReply[T](implicit tag: ClassTag[T]) {
    expectMsgClass(20.seconds, tag.runtimeClass)
  }

}
