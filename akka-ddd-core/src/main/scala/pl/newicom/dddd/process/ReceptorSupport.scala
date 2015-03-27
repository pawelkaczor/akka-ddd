package pl.newicom.dddd.process

import akka.actor.{ActorRef, Props}
import pl.newicom.dddd.actor.CreationSupport

object ReceptorSupport {

  /**
   * Responsible of creating [[Receptor]] using provided [[ReceptorConfig]]
   */
  type ReceptorFactory = (ReceptorConfig) => Receptor

  def registerReceptor(receptorConfig: ReceptorConfig)(implicit cs: CreationSupport, rf: ReceptorFactory): ActorRef = {
    val receptorProps = Props[Receptor](rf(receptorConfig))
    // TODO fix actor name
    cs.createChild(receptorProps, s"Receptor-${receptorConfig.stimuliSource.officeName}")
  }

}
