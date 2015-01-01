package pl.newicom.dddd.view

import akka.actor.SupervisorStrategy._
import akka.actor._

class UserGuardianStrategyConfigurator extends SupervisorStrategyConfigurator {

  def create(): SupervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 5) {
      case _: ActorKilledException               => Stop
      case _: ActorInitializationException       => Stop
      case _                                     => Restart
    }
  }
}