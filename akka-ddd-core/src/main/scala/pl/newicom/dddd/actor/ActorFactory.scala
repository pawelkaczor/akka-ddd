package pl.newicom.dddd.actor

import akka.actor._
import scala.language.implicitConversions

abstract class ActorFactory[A] {
  def getChild(name: String): Option[ActorRef]
  def createChild(props: Props, name: String): ActorRef
  def getOrCreateChild(props: Props, name: String): ActorRef =
    getChild(name).getOrElse(
      createChild(props, name)
    )
}

trait Supervisor extends ActorFactory[Any] {
  this: Actor with ActorLogging =>

  def getChild(name: String): Option[ActorRef] =
    context.child(name)

  def createChild(props: Props, name: String): ActorRef = {
    val actor: ActorRef = context.actorOf(props, name)
    log.debug(s"Actor created ${actor.path.name}")
    actor
  }
}

