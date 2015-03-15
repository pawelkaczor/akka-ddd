package pl.newicom.dddd

import _root_.akka.actor.ActorSystem

package object cluster extends ShardingSupport {

  implicit def singletonManagerFactory(implicit system: ActorSystem) = new SingletonManagerFactory
}
