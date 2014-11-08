package pl.newicom.dddd.cluster

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.ClusterSingletonManager
import pl.newicom.dddd.actor.CreationSupport

class SingletonManagerFactory(implicit system: ActorSystem) extends CreationSupport {

  override def getChild(name: String): Option[ActorRef] = throw new UnsupportedOperationException

  override def createChild(props: Props, name: String): ActorRef = {
    system.actorOf(ClusterSingletonManager.props(
      singletonProps = props,
      singletonName = name,
      terminationMessage = PoisonPill,
      role = None),
      name = s"singletonOf$name")
  }

}
