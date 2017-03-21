package pl.newicom.dddd

import akka.actor.ActorSystem
import pl.newicom.dddd.office.LocalOfficeId

package object cluster extends ShardingSupport {

  implicit def singletonManagerFactory[A : LocalOfficeId](implicit as: ActorSystem): SingletonManagerFactory[A] =
    new SingletonManagerFactory[A]

}
