package pl.newicom.dddd.actor

import akka.actor.{Actor, PoisonPill, ReceiveTimeout}

import scala.concurrent.duration._

@SerialVersionUID(1L)
case class Passivate(stopMessage: Any)

case class PassivationConfig(passivationMsg: Any = PoisonPill, inactivityTimeout: Duration = 30.minutes)

trait GracefulPassivation extends Actor {

  val pc: PassivationConfig

  override def preStart() {
    context.setReceiveTimeout(pc.inactivityTimeout)
  }

  override def unhandled(message: Any) {
    message match {
      case ReceiveTimeout => context.parent ! pc.passivationMsg
      case _ => super.unhandled(message)
    }
  }

}
