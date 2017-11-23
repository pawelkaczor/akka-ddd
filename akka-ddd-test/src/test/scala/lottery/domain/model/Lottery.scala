package lottery.domain.model

import java.time.OffsetDateTime

import lottery.domain.model.LotteryBehaviour.LotteryId
import lottery.domain.model.LotteryProtocol._
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.aggregate.error.DomainException

import scala.util.Random

object LotteryBehaviour {

  sealed trait Lottery extends Behavior[LotteryEvent, Lottery, Config]

  implicit case object UninitializedLottery extends Lottery with Uninitialized[Lottery] {

    def actions =
      handleCommand {
        case CreateLottery(id) => LotteryCreated(id)
      }
      .handleEvent {
        case LotteryCreated(_) => EmptyLottery
      }
  }

  case object EmptyLottery extends Lottery {

    /**
      * Action: reject Run command if has no participants
      * Only applicable when list of participants is empty
      */
    def canNotRunWithoutParticipants =
      handleCommand {
        // can't run if there is no participants
        case Run(_)  =>
          reject("Lottery has no participants")
      }


    /**
      * Action: add a participant
      * Applicable as long as we don't have a winner
      */
    def acceptParticipants =
      handleCommand {
        case AddParticipant(id, name) => ParticipantAdded(name, id)
      }
      .handleEvent {
        case ParticipantAdded(name, _) =>
          NonEmptyLottery(List(name))
      }

    def actions = canNotRunWithoutParticipants ++ acceptParticipants
  }

  case class NonEmptyLottery(participants: List[String]) extends Lottery {

    /**
      * Action: reject double booking. Can't add the same participant twice
      * Only applicable after adding at least one participant
      */
    def rejectDoubleBooking = {

      def hasParticipant(name: String) = participants.contains(name)

      handleCommand {
        // can't add participant twice
        case cmd: AddParticipant if hasParticipant(cmd.name) =>
          reject(s"Participant ${cmd.name} already added!")
      }
    }

    /**
      * Action: add a participant
      * Applicable as long as we don't have a winner
      */
    def acceptParticipants =
      handleCommand {
        case AddParticipant(id, name) => ParticipantAdded(name, id)
      }
      .handleEvent {
        case ParticipantAdded(name, _) => copy(participants = name :: participants)
      }

    /**
      * Action: remove participants (single or all)
      * Only applicable if Lottery has participants
      */
    def removeParticipants =
      // removing participants (single or all) produce ParticipantRemoved events
      handleCommand {
        case RemoveParticipant(id, name) => ParticipantRemoved(name, id)
        case RemoveAllParticipants(id) =>
          this.participants
            .map { name => ParticipantRemoved(name, id) }
      }
      .handleEvent {
        case ParticipantRemoved(name, id) =>
          val newParticipants = participants.filter(_ != name)
          // NOTE: if last participant is removed, transition back to EmptyLottery
          if (newParticipants.isEmpty)
            EmptyLottery
          else
            copy(participants = newParticipants)
      }

    /**
      * Action: run the lottery
      * Only applicable if it has at least one participant
      */
    def runTheLottery =
      handleCommand {
        case Run(id) =>
          val index = Random.nextInt(participants.size)
          val winner = participants(index)
          WinnerSelected(winner, OffsetDateTime.now, id)
      }
      .handleEvent {
        // transition to end state on winner selection
        case WinnerSelected(winner, _, id) => FinishedLottery(winner, id)
      }

    def actions = rejectDoubleBooking ++ acceptParticipants ++ removeParticipants ++ runTheLottery
  }

  case class FinishedLottery(winner: String, id: LotteryId) extends Lottery {

    /**
      * Action: reject all
      * Applicable when a winner is selected. No new commands should be accepts.
      */
    def rejectAllCommands =
      handleCommand {
        // no command can be accepted after having selected a winner
        case anyCommand  =>
          reject (new LotteryHasAlreadyAWinner(s"Lottery has already a winner and the winner is $winner"))
      }

    def actions = rejectAllCommands
  }

  type LotteryId = AggregateId
}


/** Defines the Lottery Protocol, all Commands it may receive and Events it may emit */
object LotteryProtocol {

  // Commands ============================================================
  sealed trait LotteryCommand extends Command {
    def id: LotteryId
    override def aggregateId: AggregateId = id

  }
  // Creation Command
  case class CreateLottery(id: LotteryId) extends LotteryCommand

  // Update Commands
  case class AddParticipant(id: LotteryId, name: String) extends LotteryCommand

  case class RemoveParticipant(id: LotteryId, name: String) extends LotteryCommand

  case class RemoveAllParticipants(id: LotteryId) extends LotteryCommand

  case class Run(id: LotteryId) extends LotteryCommand

  // Events ============================================================
  sealed trait LotteryEvent {
    def lotteryId: LotteryId
  }

  // Creation Event
  case class LotteryCreated(lotteryId: LotteryId) extends LotteryEvent
  // Update Events
  sealed trait LotteryUpdateEvent extends LotteryEvent
  case class ParticipantAdded(name: String, lotteryId: LotteryId) extends LotteryUpdateEvent
  case class ParticipantRemoved(name: String, lotteryId: LotteryId) extends LotteryUpdateEvent
  case class WinnerSelected(winner: String, date: OffsetDateTime, lotteryId: LotteryId) extends LotteryUpdateEvent

}

class LotteryHasAlreadyAWinner(msg: String) extends DomainException(msg)