package lottery.domain.model

import akka.actor.Props
import lottery.domain.model.LotteryBehaviour.LotteryId
import lottery.domain.model.LotteryProtocol._
import lottery.domain.model.LotterySpecSupport._
import org.scalacheck.Gen
import pl.newicom.dddd.aggregate.{AggregateRootActorFactory, DefaultConfig}
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig.testSystem

object LotterySpecSupport {
  implicit def actorFactory: AggregateRootActorFactory[LotteryAggregateRoot] =
    AggregateRootActorFactory(pc => Props(new LotteryAggregateRoot(DefaultConfig(pc))))
}

class LotterySpecSupport extends OfficeSpec[LotteryEvent, LotteryAggregateRoot](Some(testSystem)) {

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
