package lottery.domain

import pl.newicom.dddd.office.LocalOfficeId

package object model {
  implicit val lotteryOfficeId = new LocalOfficeId[LotteryAggregateRoot](classOf[LotteryAggregateRoot].getSimpleName, "lottery")

}
