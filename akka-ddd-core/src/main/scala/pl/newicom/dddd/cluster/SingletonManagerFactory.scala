package pl.newicom.dddd.cluster

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonProxySettings, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonManager}
import pl.newicom.dddd.actor.CreationSupport

class SingletonManagerFactory(implicit system: ActorSystem) extends CreationSupport {

  override def getChild(name: String): Option[ActorRef] = throw new UnsupportedOperationException

  override def createChild(props: Props, name: String): ActorRef = {
    val singletonManagerName: String = s"singletonOf$name"
    val managerSettings = ClusterSingletonManagerSettings(system).withSingletonName(name)
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = props,
        terminationMessage = PoisonPill,
        managerSettings
      ),
      name = singletonManagerName)

    val proxySettings = ClusterSingletonProxySettings(system).withSingletonName(name)
    system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$singletonManagerName",
        proxySettings),
      name = s"${name}Proxy")
  }
}