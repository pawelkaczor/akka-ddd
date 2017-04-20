package pl.newicom.dddd.test.dummy

import akka.actor.ActorRef
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootSupport.Reject
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.test.dummy.DummyProtocol._
import pl.newicom.dddd.test.dummy.ValueGeneratorActor.GenerateRandom
import pl.newicom.dddd.utils.UUIDSupport.uuidObj

import scala.concurrent.duration._

object DummyAggregateRoot extends AggregateRootSupport {

  sealed trait DummyBehaviour extends AggregateActions[DummyEvent, DummyBehaviour] {
    def isActive = false

    def rejectNegative(value: Int) = rejectIf(value < 0, "negative value not allowed")
  }

  sealed trait Dummy extends DummyBehaviour

  implicit case object Uninitialized extends DummyBehaviour with Uninitialized[DummyBehaviour] {

    def actions: Actions =

      handleCommands {
        case CreateDummy(id, name, description, value) =>
          rejectNegative(value) orElse
            DummyCreated(id, name, description, value)
      }

      .handleEvents {
        case DummyCreated(_, _, _, value) =>
          Active(value, 0)
      }
  }

  case class Active(value: Int, version: Long) extends Dummy {

    override def isActive: Boolean = true

    def actions: Actions =

      handleCommands {
        case ChangeName(id, name) =>
          NameChanged(id, name)

        case ChangeDescription(id, description) =>
          DescriptionChanged(id, description)

        case ChangeValue(id, newValue) =>
          rejectNegative(newValue) orElse
            ValueChanged(id, newValue, version + 1)

        case Reset(id, name) =>
          NameChanged(id, name) & ValueChanged(id, 0, version + 1)
      }

      .handleEvents {
        case ValueChanged(_, newValue, newVersion) =>
          copy(value = newValue, version = newVersion)

        case ValueGenerated(_, newValue, confirmationToken) =>
          WaitingForConfirmation(value, CandidateValue(newValue, confirmationToken), version)

        case _: NameChanged => this

        case _: DescriptionChanged => this
      }
  }

  case class WaitingForConfirmation(value: Int, candidateValue: CandidateValue, version: Long) extends Dummy {

    def actions: Actions =

      handleCommands {
        case ConfirmGeneratedValue(id, confirmationToken) =>
          rejectIf(candidateValue.confirmationToken != confirmationToken, "Invalid confirmation token") orElse
            ValueChanged(id, candidateValue.value, version + 1)
      }

      .handleEvents {
        case ValueChanged(_, newValue, newVersion) =>
          Active(value = newValue, version = newVersion)
      }

  }
}

import pl.newicom.dddd.test.dummy.DummyAggregateRoot._

class DummyAggregateRoot extends AggregateRoot[DummyEvent, DummyBehaviour, DummyAggregateRoot] {
  this: EventPublisher =>

  val valueGeneratorActor: ActorRef = context.actorOf(ValueGeneratorActor.props(valueGenerator))

  override def handleCommand: HandleCommand = super.handleCommand.orElse {
      case GenerateValue(_) if state.isActive =>
        valueGeneration
  }

  private def valueGeneration: Collaboration = {
    implicit val timeout = 1.seconds
    (valueGeneratorActor !< GenerateRandom) {
      case ValueGeneratorActor.ValueGenerated(value) =>
        state.rejectNegative(value) orElse
          ValueGenerated(id, value, confirmationToken = uuidObj) match {
            case Reject(_) => valueGeneration
            case r => r
        }
    }
  }

  def valueGenerator: Int = (Math.random() * 100).toInt - 50 //  -50 < v < 50

  override val pc = PassivationConfig()
}