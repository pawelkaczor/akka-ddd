package pl.newicom.dddd.office

import akka.actor._
import pl.newicom.dddd.actor.{ActorContextCreationSupport, BusinessEntityActorFactory, Passivate, PassivationConfig}
import pl.newicom.dddd.aggregate.{BusinessEntity, Command}
import pl.newicom.dddd.messaging.AddressableMessage
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.correlation.EntityIdResolution
import pl.newicom.dddd.utils.UUIDSupport.uuid7

import scala.concurrent.duration._

object SimpleOffice {

  implicit def simpleOfficeFactory[A <: BusinessEntity: BusinessEntityActorFactory: EntityIdResolution : LocalOfficeId](implicit system: ActorSystem): OfficeFactory[A] = {
    new OfficeFactory[A] {
      override def getOrCreate(): ActorRef = {
        system.actorOf(Props(new SimpleOffice[A]()), s"${officeId.id}_$uuid7")
      }
    }
  }
}

class SimpleOffice[A <: BusinessEntity: LocalOfficeId](
    inactivityTimeout: Duration = 1.minutes)(
    implicit caseIdResolution: EntityIdResolution[A],
    clerkFactory: BusinessEntityActorFactory[A])
  extends ActorContextCreationSupport with Actor with ActorLogging {

  val clerkProps = clerkFactory.props(PassivationConfig(Passivate(PoisonPill), clerkFactory.inactivityTimeout))

  override def aroundReceive(receive: Actor.Receive, msg: Any): Unit = {
    receive.applyOrElse(msg match {
      case c: Command => CommandMessage(c)
      case other => other
    }, unhandled)
  }

  def receive: Receive = {
    // TODO (passivation) in-between receiving Passivate and Terminated the office should buffer all incoming messages
    // for the clerk being passivated, when receiving Terminated it should flush the buffer
    case Passivate(stopMessage) =>
      dismiss(sender(), stopMessage)
    case msg: AddressableMessage =>
      val caseId: String = resolveCaseId(msg)
      val clerk = assignClerk(clerkProps, caseId)
      log.debug(s"Forwarding message to ${clerk.path}")
      clerk forward msg
  }

  def resolveCaseId(msg: Any) = caseIdResolution.entityIdResolver(msg)

  def assignClerk(caseProps: Props, caseId: String): ActorRef = getOrCreateChild(caseProps, caseId)

  def dismiss(clerk: ActorRef, stopMessage: Any) {
    log.info(s"Passivating $sender()")
    clerk ! stopMessage
  }
}
