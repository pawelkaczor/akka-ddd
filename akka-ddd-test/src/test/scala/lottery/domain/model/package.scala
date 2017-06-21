package lottery.domain

import lottery.domain.model.LotteryBehaviour.Lottery
import lottery.domain.model.LotteryProtocol.LotteryEvent
import pl.newicom.dddd.aggregate.{AggregateRoot, Config, ConfigClass, ReplyWithEvents}
import pl.newicom.dddd.office.LocalOfficeId

package object model {
  implicit val lotteryOfficeId = new LocalOfficeId[LotteryAggregateRoot](classOf[LotteryAggregateRoot].getSimpleName, "lottery")

  class LotteryAggregateRoot(val config: Config) extends AggregateRoot[LotteryEvent, Lottery, LotteryAggregateRoot]
    with ReplyWithEvents
    with ConfigClass[Config]
}
