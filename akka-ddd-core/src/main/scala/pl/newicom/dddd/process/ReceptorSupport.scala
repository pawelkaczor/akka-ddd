package pl.newicom.dddd.process

import akka.actor.{ActorRef, Props}
import pl.newicom.dddd.actor.CreationSupport
import pl.newicom.dddd.coordination.ReceptorConfig

object ReceptorSupport {

  /**
   * Responsible of creating [[Receptor]] using provided ReceptorConfig
   */
  type ReceptorFactory = (ReceptorConfig) => Receptor

  def receptor(receptorConfig: ReceptorConfig)(implicit cs: CreationSupport, rf: ReceptorFactory): ActorRef = {
    val receptorProps = Props[Receptor](rf(receptorConfig))
    cs.createChild(receptorProps, s"Receptor-${receptorConfig.stimuliSource.id}")
  }

}
