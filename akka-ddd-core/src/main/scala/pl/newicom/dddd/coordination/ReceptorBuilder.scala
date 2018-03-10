package pl.newicom.dddd.coordination

import akka.actor.ActorPath
import pl.newicom.dddd.Eventsourced
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.coordination.ReceptorConfig.{ReceiverResolver, StimuliSource, Transduction}
import pl.newicom.dddd.messaging.Message
import pl.newicom.dddd.messaging.MetaAttribute.Target
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.{LocalOfficeId, OfficeRegistryImpl}

object ReceptorConfig {
  type Transduction     = PartialFunction[EventMessage, Message]
  type ReceiverResolver = PartialFunction[Message, ActorPath]
  type StimuliSource    = Eventsourced
}

case class ReceptorConfig(
    receptorId: EntityId,
    stimuliSource: StimuliSource,
    transduction: Transduction,
    receiverResolver: ReceiverResolver,
    capacity: Int,
    isSupporting_MustFollow_Attribute: Boolean = true
)

trait ReceptorGrammar {
  def reactTo[A: LocalOfficeId]: ReceptorGrammar
  def applyTransduction(transduction: Transduction): ReceptorGrammar
  def route(receiverResolver: ReceiverResolver): ReceptorConfig
  def propagateTo(receiver: ActorPath): ReceptorConfig
}

case class ReceptorBuilder(
      id: EntityId,
      stimuliSource: StimuliSource = null,
      transduction: Transduction = { case em => em },
      receiverResolver: ReceiverResolver = null,
      capacity: Int = 1000)
    extends ReceptorGrammar {

  def reactTo[A: LocalOfficeId]: ReceptorBuilder =
    reactTo(implicitly[LocalOfficeId[A]])

  def reactTo(observable: Eventsourced): ReceptorBuilder =
    copy(stimuliSource = observable)

  def applyTransduction(transduction: Transduction): ReceptorBuilder =
    copy(transduction = transduction)

  def autoRoute: ReceptorConfig = route {
    case msg: Message =>
      msg.tryGetMetaAttribute(Target) match {
        case Some(t) => ActorPath.fromString(t)
        case _ => throw new RuntimeException("Resolving receiver failed")
      }
  }

  def autoRoute(officeRegistry: OfficeRegistryImpl): ReceptorConfig =
    applyTransduction {
      case em =>
        transduction(em) match {
          case cm @ CommandMessage(command, metadata) if !metadata.contains(Target) =>
            cm.copy(command = new ToDeliverableCommandTransformation(officeRegistry)(command))
          case msg => msg
       }
    }.route {
      case msg: Message =>
        msg.tryGetMetaAttribute(Target).map(ActorPath.fromString).getOrElse {
          msg match {
            case cm: CommandMessage =>
              officeRegistry.commandHandler(cm.command).actorPath
            case _ =>
              throw new RuntimeException("Resolving receiver failed")
          }
        }
    }

  def route(receiverResolver: ReceiverResolver): ReceptorConfig =
    ReceptorConfig(id, stimuliSource, transduction, receiverResolver, capacity)

  def propagateTo(receiver: ActorPath): ReceptorConfig =
    route({ case _ => receiver })

  def withCapacity(capacity: Int): ReceptorBuilder =
    copy(capacity = capacity)
}
