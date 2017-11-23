package lottery.domain.model

import com.softwaremill.tagging._
import akka.actor.Props
import com.softwaremill.tagging.@@
import lottery.domain.model.LotteryBehaviour.LotteryId
import lottery.domain.model.LotteryProtocol._
import lottery.domain.model.LotterySpecSupport._
import org.scalacheck.{Arbitrary, Gen}
import pl.newicom.dddd.aggregate.{AggregateRootActorFactory, DefaultConfig}
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig.testSystem

object LotterySpecSupport {
  implicit def actorFactory: AggregateRootActorFactory[LotteryAggregateRoot] =
    AggregateRootActorFactory(pc => Props(new LotteryAggregateRoot(DefaultConfig(pc))))
}

class LotterySpecSupport extends OfficeSpec[LotteryEvent, LotteryAggregateRoot](Some(testSystem)) {

  def lotteryId: LotteryId = aggregateId

  // participants
  trait Paul
  trait John

  implicit def AddPaulParticipant: A[AddParticipant @@ Paul] = Arbitrary {
    for {name <- Gen.const("Paul")} yield {
      AddParticipant(lotteryId, name).taggedWith[Paul]
    }
  }

  implicit def AddJohnParticipant: A[AddParticipant @@ John] = Arbitrary {
    for {name <- Gen.const("John")} yield {
      AddParticipant(lotteryId, name).taggedWith[John]
    }
  }

}
