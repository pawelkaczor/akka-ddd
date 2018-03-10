package lottery.domain.model

import org.scalacheck.ScalacheckShapeless._
import com.softwaremill.tagging.@@
import lottery.domain.model.LotteryProtocol._
import pl.newicom.dddd.aggregate.error.DomainException

class LotterySpec extends LotterySpecSupport {

  "Lottery should" should {

    "run" in {
      given {
        a_list_of [CreateLottery, AddParticipant @@ Paul, AddParticipant @@ John]
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
        a_list_of[CreateLottery, AddParticipant @@ Paul, AddParticipant @@ John, Run]
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
      given {
        a_list_of [CreateLottery, AddParticipant @@ John]
      }
      .when {
        a [AddParticipant @@ John]
      }
      .expectException[DomainException](s"Participant John already added!")
    }

    "reset" in {
      given {
        a_list_of [CreateLottery, AddParticipant @@ John, AddParticipant @@ Paul]
      }
      .when {
        a [RemoveAllParticipants]
      }
      .expect { implicit hist =>
        ParticipantRemoved(first[ParticipantAdded].name, lotteryId) &
          ParticipantRemoved(last[ParticipantAdded].name, lotteryId)
      }
    }

    "not reset if has a winner" in {
      given {
        a_list_of [CreateLottery, AddParticipant @@ John, AddParticipant @@ Paul, Run]
      }
      .when {
        a [RemoveAllParticipants]
      }
      .expectException[LotteryHasAlreadyAWinner]()
    }

    "not add participant if has a winner" in {
      given {
        a_list_of [CreateLottery, AddParticipant @@ John, AddParticipant @@ Paul, Run]
      }
      .when {
        a [AddParticipant @@ John]
      }
      .expectException[LotteryHasAlreadyAWinner]()
    }

  }

}
