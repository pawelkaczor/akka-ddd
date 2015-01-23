package pl.newicom.dddd.writefront

import akka.actor._
import akka.contrib.pattern.ClusterClient

import scala.collection.mutable

trait GlobalOfficeClientSupport {
  this: Actor =>

  def contactPoints: Seq[String]

  private lazy val officeClientMap: mutable.Map[String, ActorRef] = mutable.Map.empty

  private lazy val clusterClient: ActorRef = {
    val initialContacts = contactPoints.map {
      case AddressFromURIString(address) ⇒ system.actorSelection(RootActorPath(address) / "user" / "receptionist")
    }
    system.actorOf(ClusterClient.props(initialContacts.toSet), "clusterClient")
  }

  def office(officeName: String) =
    officeClientMap.getOrElseUpdate(officeName, system.actorOf(Props(new OfficeClient(officeName))))

  class OfficeClient(officeName: String) extends Actor {
    override def receive: Receive = {
      case msg => clusterClient forward ClusterClient.Send(s"/user/sharding/$officeName", msg, localAffinity = true)
    }
  }

  private def system = context.system
}
