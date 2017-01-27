package pl.newicom.dddd.test.dummy

import akka.actor.ActorRef
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootSupport.Eventually
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.test.dummy.DummyProtocol._
import pl.newicom.dddd.test.dummy.ValueGeneratorActor.GenerateRandom
import pl.newicom.dddd.utils.UUIDSupport.uuidObj

import scala.concurrent.duration._
import scala.util.control.NonFatal

object DummyAggregateRoot extends AggregateRootSupport[DummyEvent] {

  val ValueGeneration = "VG"
  type Collaborations = Map[String, Eventually[DummyEvent]]

  sealed trait DummyState extends AggregateState[DummyState] {
    def validate(v: Int): Unit = if (v < 0) sys.error("negative value not allowed")
    def handleCommand(c: Collaborations): HandleCommand
  }

  sealed trait Dummy extends DummyState

  implicit case object Uninitialized extends DummyState with Uninitialized[DummyState] {

    def handleCommand(c: Collaborations) = {
      case CreateDummy(id, name, description, value) =>
        validate(value)
        DummyCreated(id, name, description, value)
    }

    def apply = {
      case DummyCreated(_, _, _, value) =>
        Active(value, 0)
    }
  }

  case class Active(value: Int, version: Long) extends Dummy {

    def handleCommand(c: Collaborations) = {
      case ChangeName(id, name) =>
        NameChanged(id, name)

      case ChangeDescription(id, description) =>
        DescriptionChanged(id, description)

      case ChangeValue(id, value) =>
        validate(value)
        ValueChanged(id, value, version + 1)

      case GenerateValue(_) =>
        c(ValueGeneration)

      case Reset(id, name) =>
        NameChanged(id, name) & ValueChanged(id, 0, version + 1)
    }

    def apply = {
      case ValueChanged(_, newValue, newVersion) =>
        copy(value = newValue, version = newVersion)

      case ValueGenerated(_, newValue, confirmationToken) =>
        WaitingForConfirmation(value, CandidateValue(newValue, confirmationToken), version)

      case _: NameChanged => this

      case _: DescriptionChanged => this
    }
  }

  case class WaitingForConfirmation(value: Int, candidateValue: CandidateValue, version: Long) extends Dummy {

    def handleCommand(c: Collaborations) = {
      case ConfirmGeneratedValue(id, confirmationToken) =>
        if (candidateValue.confirmationToken == confirmationToken) {
          ValueChanged(id, candidateValue.value, version + 1)
        } else {
          sys.error("Invalid confirmation token")
        }
    }

    def apply = {
      case ValueChanged(_, newValue, newVersion) =>
        Active(value = newValue, version = newVersion)
    }
  }
}

import pl.newicom.dddd.test.dummy.DummyAggregateRoot._

class DummyAggregateRoot extends AggregateRoot[DummyEvent, DummyState, DummyAggregateRoot] {
  this: EventPublisher =>

  val valueGeneratorActor: ActorRef = context.actorOf(ValueGeneratorActor.props(valueGenerator))

  def handleCommand: HandleCommand = state.handleCommand(Map(ValueGeneration -> valueGeneration))

  private def valueGeneration: Collaboration = {
    implicit val timeout = 1.seconds
    (valueGeneratorActor !< GenerateRandom) {
      case ValueGeneratorActor.ValueGenerated(value) =>
        try {
          state.validate(value)
          ValueGenerated(id, value, confirmationToken = uuidObj)
        } catch {
            case NonFatal(_) => // try again
              valueGeneration
        }
    }
  }

  def valueGenerator: Int = (Math.random() * 100).toInt - 50 //  -50 < v < 50

  override val pc = PassivationConfig()
}