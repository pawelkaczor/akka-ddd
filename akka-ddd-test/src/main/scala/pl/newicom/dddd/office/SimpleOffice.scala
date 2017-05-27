package pl.newicom.dddd.office

import akka.actor._
import pl.newicom.dddd.actor.{ActorContextCreationSupport, BusinessEntityActorFactory, Passivate, PassivationConfig}
import pl.newicom.dddd.aggregate.{BusinessEntity, Command, EntityId, Query}
import pl.newicom.dddd.messaging.AddressableMessage
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.correlation.AggregateIdResolution
import pl.newicom.dddd.messaging.query.QueryMessage
import pl.newicom.dddd.office.SimpleOffice.Batch
import pl.newicom.dddd.utils.UUIDSupport.uuid7

object SimpleOffice {

  implicit def simpleOfficeFactory[A <: BusinessEntity: BusinessEntityActorFactory : LocalOfficeId](implicit system: ActorSystem): OfficeFactory[A] = {
    new OfficeFactory[A] {
      override def getOrCreate(): ActorRef = {
        system.actorOf(Props(new SimpleOffice[A]), s"${officeId.id}_$uuid7")
      }
    }
  }

  case class Batch[A <: AddressableMessage](msgs: Seq[A])

}

class SimpleOffice[A <: BusinessEntity: LocalOfficeId](
    implicit clerkFactory: BusinessEntityActorFactory[A])
  extends ActorContextCreationSupport with Actor with ActorLogging {

  val caseIdResolution = new AggregateIdResolution
  val clerkProps: Props = clerkFactory.props(PassivationConfig(Passivate(PoisonPill), clerkFactory.inactivityTimeout))

  override def aroundReceive(receive: Actor.Receive, msg: Any): Unit = {
    receive.applyOrElse(msg match {
      case c: Command => CommandMessage(c)
      case q: Query => QueryMessage(q)
      case other => other
    }, unhandled)
  }

  def receive: Receive = {
    // TODO (passivation) in-between receiving Passivate and Terminated the office should buffer all incoming messages
    // for the clerk being passivated, when receiving Terminated it should flush the buffer
    case Passivate(stopMessage) =>
      dismiss(sender(), stopMessage)
    case msg: AddressableMessage =>
      forwardToClerk(msg)
    case Batch(msgs) =>
      msgs foreach forwardToClerk
  }

  def forwardToClerk(msg: AddressableMessage): Unit = {
    val caseId: String = resolveCaseId(msg)
    val clerk = assignClerk(clerkProps, caseId)
    log.debug(s"Forwarding message to ${clerk.path}")
    clerk forward msg
  }

  def resolveCaseId(msg: Any): EntityId = caseIdResolution.entityIdResolver(msg)

  def assignClerk(caseProps: Props, caseId: String): ActorRef = getOrCreateChild(caseProps, caseId)

  def dismiss(clerk: ActorRef, stopMessage: Any) {
    log.info(s"Passivating $sender()")
    clerk ! stopMessage
  }
}
