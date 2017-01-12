package pl.newicom.dddd.test.dummy

import java.util.UUID

import akka.actor.ActorRef
import pl.newicom.dddd.actor.PassivationConfig
import DummyProtocol._
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.test.dummy.DummyAggregateRoot._
import pl.newicom.dddd.test.dummy.ValueGeneratorActor.GenerateRandom
import pl.newicom.dddd.utils.UUIDSupport.uuidObj

import scala.concurrent.duration._

object DummyAggregateRoot {

  sealed trait DummyState extends AggregateState[DummyState] {
    def validate(v: Int): Unit = if (v < 0) sys.error("negative value not allowed")
  }

  implicit case object Uninitialized extends DummyState with Uninitialized[DummyState] {
    def apply: StateMachine = {
      case DummyCreated(_, _, _, value) => Active(value)
    }
  }

  sealed trait Dummy extends DummyState

  case class Active(value: Int) extends Dummy {

    def apply: StateMachine = {
      case ValueChanged(_, newValue, _) =>
        copy(value = newValue)
      case ValueGenerated(_, newValue, confirmationToken) =>
        WaitingForConfirmation(value, CandidateValue(newValue, confirmationToken))
      case _: NameChanged => this
      case _: DescriptionChanged => this
    }

  }

  case class WaitingForConfirmation(value: Int, candidateValue: CandidateValue) extends Dummy {
    def apply: StateMachine = {
      case ValueChanged(_, newValue, _) =>
        Active(value = newValue)
    }
  }
}

class DummyAggregateRoot extends AggregateRoot[DummyState, DummyAggregateRoot] {
  this: EventPublisher =>

  val valueGeneratorActor: ActorRef = context.actorOf(ValueGeneratorActor.props(valueGenerator))

  override def handleCommand: Receive = state match {

    case s: Uninitialized.type => {
      case CreateDummy(id, name, description, value) =>
        s.validate(value)
        raise(DummyCreated(id, name, description, value))
    }

    case s @ Active(currentValue) => {

      case ChangeName(id, name) =>
        raise(NameChanged(id, name))

      case ChangeDescription(id, description) =>
        raise(DescriptionChanged(id, description))

      case ChangeValue(id, value) =>
        s.validate(value)
        raise(ValueChanged(id, value, lastSequenceNr))

      case GenerateValue(id) =>
        implicit val timeout = 3.seconds
        (valueGeneratorActor !< GenerateRandom) {
          case ValueGeneratorActor.ValueGenerated(value) =>
            s.validate(value)
            raise(ValueGenerated(id, value, confirmationToken = generateConfirmationToken))
        }
    }

    case WaitingForConfirmation(currentValue, candidateValue) => {

      case ConfirmGeneratedValue(id, confirmationToken) =>
        if (candidateValue.confirmationToken == confirmationToken) {
          raise(ValueChanged(id, candidateValue.value, lastSequenceNr))
        }
    }

  }

  def generateConfirmationToken: UUID = uuidObj

  def valueGenerator: Int = (Math.random() * 100).toInt

  override val pc = PassivationConfig()
}