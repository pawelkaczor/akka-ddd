package pl.newicom.dddd.process

import akka.actor.{ActorRef, ActorSystem, Props}
import pl.newicom.dddd.actor.ActorFactory
import pl.newicom.dddd.coordination.ReceptorConfig
import pl.newicom.dddd.office.LocalOfficeId

abstract class ReceptorActorFactory[A : LocalOfficeId : ActorFactory](implicit system: ActorSystem) {

  type ReceptorFactory = ReceptorConfig => Receptor

  protected def receptorFactory: ReceptorFactory

  def apply(receptorConfig: ReceptorConfig): ActorRef = {
    val receptorProps = Props[Receptor](receptorFactory(receptorConfig))
    implicitly[ActorFactory[A]].createChild(receptorProps, s"Receptor-${receptorConfig.receptorId}")
  }

}