package pl.newicom.dddd.cluster

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.office.LocalOfficeId

class SingletonManagerFactory[A : LocalOfficeId](implicit system: ActorSystem) extends CreationSupport[A] {

  override def getChild(name: String): Option[ActorRef] = throw new UnsupportedOperationException

  override def createChild(props: Props, name: String): ActorRef = {
    val singletonManagerName: String = s"singletonOf$name"
    val department = implicitly[LocalOfficeId[A]].department

    val managerSettings = ClusterSingletonManagerSettings(system)
      .withSingletonName(name)
      .withRole(department)

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = props,
        terminationMessage = PoisonPill,
        managerSettings
      ),
      name = singletonManagerName)

    val proxySettings = ClusterSingletonProxySettings(system)
      .withSingletonName(name)
      .withRole(department)

    system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$singletonManagerName",
        proxySettings),
      name = s"${name}Proxy")
  }
}