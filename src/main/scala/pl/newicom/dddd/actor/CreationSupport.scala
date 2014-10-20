package pl.newicom.dddd.actor

import akka.actor._

import scala.language.implicitConversions

trait CreationSupport {
  def getChild(name: String): Option[ActorRef]
  def createChild(props: Props, name: String): ActorRef
  def getOrCreateChild(props: Props, name: String): ActorRef = getChild(name).getOrElse(createChild(props, name))
}

trait ActorContextCreationSupport extends CreationSupport {
  this: ActorLogging =>
  def context: ActorContext

  def getChild(name: String): Option[ActorRef] = context.child(name)
  def createChild(props: Props, name: String): ActorRef = {
    val actor: ActorRef = context.actorOf(props, name)
    log.info(s"Actor created $actor")
    actor
  }
}