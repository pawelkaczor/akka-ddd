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
import scala.util.control.NonFatal

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

class DummyAggregateRoot extends AggregateRoot[DummyEvent, DummyState, DummyAggregateRoot] {
  this: EventPublisher =>

  val valueGeneratorActor: ActorRef = context.actorOf(ValueGeneratorActor.props(valueGenerator))

  def handleCommand: HandleCommand = state match {

    case s: Uninitialized.type => {
      case CreateDummy(id, name, description, value) =>
        s.validate(value)
        DummyCreated(id, name, description, value)
    }

    case s @ Active(_) => {

      case ChangeName(id, name) =>
        NameChanged(id, name)

      case ChangeDescription(id, description) =>
        DescriptionChanged(id, description)

      case ChangeValue(id, value) =>
        s.validate(value)
        ValueChanged(id, value, lastSequenceNr)

      case GenerateValue(_) =>
        valueGeneration

      case Reset(id, name) =>
        NameChanged(id, name) & ValueChanged(id, 0, lastSequenceNr)
    }

    case WaitingForConfirmation(_, candidateValue) => {

      case ConfirmGeneratedValue(id, confirmationToken) =>
        if (candidateValue.confirmationToken == confirmationToken) {
          ValueChanged(id, candidateValue.value, lastSequenceNr)
        } else {
          sys.error("Invalid confirmation token")
        }
    }

  }

  private def valueGeneration: Collaboration = {
    implicit val timeout = 1.seconds
    (valueGeneratorActor !< GenerateRandom) {
      case ValueGeneratorActor.ValueGenerated(value) =>
        try {
          state.validate(value)
          ValueGenerated(id, value, confirmationToken = generateConfirmationToken)
        } catch {
            case NonFatal(_) => // try again
              valueGeneration
        }
    }
  }

  def generateConfirmationToken: UUID = uuidObj

  def valueGenerator: Int = (Math.random() * 100).toInt - 50 //  -50 < v < 50

  override val pc = PassivationConfig()
}