package pl.newicom.dddd

import _root_.akka.actor.ActorSystem

package object cluster {

  implicit def singletonManagerFactory(implicit system: ActorSystem) = new SingletonManagerFactory
}
