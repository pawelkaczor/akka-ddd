package pl.newicom.dddd.test.dummy

import akka.actor.ActorRef
import pl.newicom.dddd.actor._
import pl.newicom.dddd.aggregate.AggregateRootSupport.{NoReaction, Reaction, RejectConditionally}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.persistence.{RegularSnapshotting, RegularSnapshottingConfig}
import pl.newicom.dddd.test.dummy.DummyProtocol._
import pl.newicom.dddd.test.dummy.ValueGeneratorActor.GenerateRandom
import pl.newicom.dddd.utils.UUIDSupport.uuidObj

import scala.concurrent.duration._

object DummyAggregateRoot extends AggregateRootSupport {

  val defaultValueGenerator: () => Int =
    () => (Math.random() * 100).toInt - 50 //  -50 < v < 50

  case class DummyConfig(pc: PassivationConfig, valueGenerator: () => Int = defaultValueGenerator)

  case class DummyRuntimeConfig(pc: PassivationConfig, generateEventuallyValid: Reaction[DummyEvent], generateRandom: Reaction[DummyEvent]) extends Config {
    def respondingPolicy: RespondingPolicy = ReplyWithEvents
  }

  sealed trait Dummy extends Behavior[DummyEvent, Dummy, DummyRuntimeConfig] {
    def isActive = false

    def rejectInvalid(value: Int): RejectConditionally =
      rejectIf(value < 0, "negative value not allowed")
  }

  implicit case object Uninitialized extends Dummy with Uninitialized[Dummy] {

    def actions: Actions =
      handleCommand {
        case CreateDummy(id, name, description, value) =>
          rejectInvalid(value.value) orElse
            DummyCreated(id, name, description, value.value)
      }.handleEvent {
          case DummyCreated(_, _, _, value) =>
            Active(value, 0)
        }
  }

  case class Active(value: Int, version: Long) extends Dummy {

    override def isActive: Boolean = true

    def actions: Actions =
      withContext { ctx =>
        handleCommand {
          case ChangeName(id, name) =>
            NameChanged(id, name)

          case ChangeDescription(id, description) =>
            DescriptionChanged(id, description)

          case ChangeValue(id, newValue) =>
            rejectInvalid(newValue) orElse
              ValueChanged(id, newValue, version + 1)

          case Reset(id, name) =>
            NameChanged(id, name) & ValueChanged(id, 0, version + 1)

          case GenerateValue(_) =>
            ctx.config.generateRandom
              .flatMapMatching {
                case ValueGenerated(_, value, _) =>
                  rejectInvalid(value) orElse NoReaction
              }
              .recoverWith(ctx.config.generateEventuallyValid)

        }
      }.handleEvent {
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
      handleCommand {
        case ConfirmGeneratedValue(id, confirmationToken) =>
          rejectIf(candidateValue.confirmationToken != confirmationToken, "Invalid confirmation token") orElse
            ValueChanged(id, candidateValue.value, version + 1)
      }.handleEvent {
          case ValueChanged(_, newValue, newVersion) =>
            Active(value = newValue, version = newVersion)
      }

  }
}

import pl.newicom.dddd.test.dummy.DummyAggregateRoot._

class DummyAggregateRoot(cfg: DummyConfig)
    extends AggregateRoot[DummyEvent, Dummy, DummyAggregateRoot]
    with AggregateRootLogger[DummyEvent]
    with ConfigClass[DummyRuntimeConfig] {

  val config = DummyRuntimeConfig(cfg.pc, generateEventuallyValid, generateRandom)

  lazy val valueGeneratorActor: ActorRef = context.actorOf(ValueGeneratorActor.props(cfg.valueGenerator))

  private def generateRandom: Reaction[DummyEvent] = {
    implicit val timeout: FiniteDuration = 10.millis
    (valueGeneratorActor !< GenerateRandom) {
      case ValueGeneratorActor.ValueGenerated(value) =>
          ValueGenerated(new DummyId(id), value, confirmationToken = uuidObj)
    }
  }

  private def generateEventuallyValid: Reaction[DummyEvent] = {
    implicit val timeout: FiniteDuration = 10.millis
    (valueGeneratorActor !< GenerateRandom) {
      case ValueGeneratorActor.ValueGenerated(value) =>
        state.rejectInvalid(value) orElse
          ValueGenerated(new DummyId(id), value, confirmationToken = uuidObj)
    }.recoverWith {
      generateEventuallyValid
    }
  }

  def snapshottingConfig = RegularSnapshottingConfig(receiveCommand, interval = 1)
}
