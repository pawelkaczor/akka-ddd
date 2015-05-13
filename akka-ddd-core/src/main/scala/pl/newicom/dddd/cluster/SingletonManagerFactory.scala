package pl.newicom.dddd.cluster

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonManager}
import pl.newicom.dddd.actor.CreationSupport

class SingletonManagerFactory(implicit system: ActorSystem) extends CreationSupport {

  override def getChild(name: String): Option[ActorRef] = throw new UnsupportedOperationException

  override def createChild(props: Props, name: String): ActorRef = {
    val singletonManagerName: String = s"singletonOf$name"
    system.actorOf(ClusterSingletonManager.props(
      singletonProps = props,
      singletonName = name,
      terminationMessage = PoisonPill,
      role = None),
      name = singletonManagerName)

    system.actorOf(ClusterSingletonProxy.props(
      singletonPath = s"/user/$singletonManagerName/$name",
      role = None),
      name = s"${name}Proxy")
  }

}
