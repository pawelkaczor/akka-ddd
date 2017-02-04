package lottery.domain.model

import akka.actor.Props
import lottery.domain.model.LotteryAggregateRoot.LotteryId
import lottery.domain.model.LotteryProtocol.{AddParticipant, CreateLottery, RemoveAllParticipants, Run}
import org.scalacheck.Gen
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.AggregateRootActorFactory
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig.testSystem
import LotterySpecSupport._
import scala.concurrent.duration._

object LotterySpecSupport {

  implicit def actorFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[LotteryAggregateRoot] =
    new AggregateRootActorFactory[LotteryAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new LotteryAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it

    }
}

class LotterySpecSupport extends OfficeSpec[LotteryAggregateRoot](Some(testSystem)) {

  def lotteryId: LotteryId = aggregateId

  var generatedParticipants: Set[String] = Set()

  override def ensureOfficeTerminated(): Unit = {
    generatedParticipants = Set()
    super.ensureOfficeTerminated()
  }

  implicit def genCreate: Gen[CreateLottery] = Gen.const(CreateLottery(lotteryId))

  implicit def genRun: Gen[Run] = Gen.const(Run(lotteryId))

  implicit def genAddParticipant: Gen[AddParticipant] = for {
    name <- Gen.oneOf("Paul", "John").suchThat(p => !generatedParticipants.contains(p))
  } yield {
    generatedParticipants = generatedParticipants + name
    AddParticipant(lotteryId, name)
  }

  implicit def genRemoveAll: Gen[RemoveAllParticipants] = Gen.const(RemoveAllParticipants(lotteryId))
}
