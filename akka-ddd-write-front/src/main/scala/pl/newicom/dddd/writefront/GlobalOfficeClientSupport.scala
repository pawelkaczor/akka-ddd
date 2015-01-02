package pl.newicom.dddd.writefront

import akka.actor._
import akka.contrib.pattern.ClusterClient

trait GlobalOfficeClientSupport {
  this: Actor =>

  def contactPoints: Seq[String]

  def office(officeName: String): ActorRef = {
    system.actorOf(Props(new OfficeClient(officeName)))
  }

  private lazy val clusterClient: ActorRef = {
    val initialContacts = contactPoints.map {
      case AddressFromURIString(address) ⇒ system.actorSelection(RootActorPath(address) / "user" / "receptionist")
    }
    system.actorOf(ClusterClient.props(initialContacts.toSet), "clusterClient")
  }

  class OfficeClient(officeName: String) extends Actor {
    override def receive: Receive = {
      case msg => clusterClient forward ClusterClient.Send(s"/user/sharding/$officeName", msg, localAffinity = true)
    }
  }

  private def system = context.system
}
