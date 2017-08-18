package pl.newicom.dddd.process

import akka.actor.{ActorRef, ActorSystem, Props}
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.coordination.ReceptorConfig
import pl.newicom.dddd.office.LocalOfficeId

abstract class ReceptorActorFactory[A : LocalOfficeId : CreationSupport](implicit system: ActorSystem) {

  type ReceptorFactory = ReceptorConfig => Receptor

  protected def receptorFactory: ReceptorFactory

  def apply(receptorConfig: ReceptorConfig): ActorRef = {
    val receptorProps = Props[Receptor](receptorFactory(receptorConfig))
    implicitly[CreationSupport[A]].createChild(receptorProps, s"Receptor-${receptorConfig.stimuliSource.id}")
  }

}