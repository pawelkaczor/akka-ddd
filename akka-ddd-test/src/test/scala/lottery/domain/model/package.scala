package lottery.domain

import lottery.domain.model.LotteryBehaviour.Lottery
import lottery.domain.model.LotteryProtocol.LotteryEvent
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate.{AggregateRoot, ReplyWithEvents}
import pl.newicom.dddd.office.LocalOfficeId

package object model {
  implicit val lotteryOfficeId = new LocalOfficeId[LotteryAggregateRoot](classOf[LotteryAggregateRoot].getSimpleName, "lottery")

  class LotteryAggregateRoot extends AggregateRoot[LotteryEvent, Lottery, LotteryAggregateRoot] with ReplyWithEvents {
    override val pc = PassivationConfig()
  }
}
