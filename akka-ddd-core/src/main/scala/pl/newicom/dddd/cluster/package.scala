package pl.newicom.dddd

import akka.actor.ActorSystem

package object cluster extends ShardingSupport {

  implicit def singletonManagerFactory(implicit as: ActorSystem): SingletonManagerFactory =
    new SingletonManagerFactory

}
