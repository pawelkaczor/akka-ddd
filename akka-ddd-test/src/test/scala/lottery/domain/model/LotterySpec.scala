package lottery.domain.model

import lottery.domain.model.LotteryProtocol._
import pl.newicom.dddd.aggregate.error.DomainException
import pl.newicom.dddd.office.Office

class LotterySpec extends LotterySpecSupport {

  def lotteryOffice: Office = officeUnderTest

  "Lottery should" should {

    "run" in {
      given {
        a_list_of [CreateLottery, AddParticipant, AddParticipant]
      }
      .when {
        a [Run]
      }
      .expectEventMatching {
        case e: WinnerSelected => e
      }
    }

    "not run twice" in {
      given {
        a_list_of[CreateLottery, AddParticipant, AddParticipant, Run]
      }
      .when {
        a [Run]
      }
      .expectException[LotteryHasAlreadyAWinner]()
    }

    "not run without participants" in {
      given {
        a [CreateLottery]
      }
      .when {
        a [Run]
      }
      .expectException[DomainException]("Lottery has no participants")
    }

    "not add twice the same participant" in {
      var participant: String = null

      given {
        a_list_of [CreateLottery, AddParticipant]
      }
      .when { implicit hist =>
        participant = past[ParticipantAdded].name

        AddParticipant(lotteryId, participant)
      }
      .expectException[DomainException](s"Participant $participant already added!")
    }

    "reset" in {
      given {
        a_list_of [CreateLottery, AddParticipant, AddParticipant]
      }
      .when {
        a [RemoveAllParticipants]
      }
      .expectEvents(
        ParticipantRemoved("Paul", lotteryId), ParticipantRemoved("John", lotteryId)
      )
    }

    "not reset if has a winner" in {
      given {
        a_list_of [CreateLottery, AddParticipant, AddParticipant, Run]
      }
      .when {
        a [RemoveAllParticipants]
      }
      .expectException[LotteryHasAlreadyAWinner]()
    }

    "not add participant if has a winner" in {
      given {
        a_list_of [CreateLottery, AddParticipant, AddParticipant, Run]
      }
      .when {
        a [AddParticipant]
      }
      .expectException[LotteryHasAlreadyAWinner]()
    }

  }

}
