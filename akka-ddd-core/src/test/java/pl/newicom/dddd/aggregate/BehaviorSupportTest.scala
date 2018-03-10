package pl.newicom.dddd.aggregate

import org.scalatest.Matchers
import pl.newicom.dddd.aggregate.AggregateRootSupport.{AcceptC, NoReaction}

class BehaviorSupportTest extends org.scalatest.WordSpec with Matchers with AggregateRootSupport {

  case object Event
  val Accept = AcceptC(Seq(Event))
  val Reject = AggregateRootSupport.Reject(new RuntimeException("test 1"))
  val Reject2 = AggregateRootSupport.Reject(new RuntimeException("test 2"))

  "NoReaction" when {

    "followed by NoReaction" should {
      "result in NoReaction" in {
        NoReaction.flatMap(_ => NoReaction) should be (NoReaction)
      }
    }

    "followed by Accept" should {
      "result in Accept" in {
        NoReaction.flatMap(_ => Accept) should be (Accept)
      }
    }

    "followed by Reject" should {
      "result in Reject" in {
        NoReaction.flatMap(_ => Reject) should be (Reject)
      }
    }
  }

  "Reject" when {

    "followed by NoReaction" should {
      "not change" in {
        Reject.flatMap(_ => NoReaction) should be (Reject)
      }
    }

    "followed by Accept" should {
      "not change" in {
        Reject.flatMap(_ => Accept) should be (Reject)
      }
    }

    "followed by another Reject" should {
      "not change" in {
        Reject.flatMap(_ => Reject2) should be (Reject)
      }
    }
  }

  "Accept" when {

    "followed by NoReaction" should {
      "not change" in {
        Accept.flatMap(_ => NoReaction) should be (Accept)
      }
    }

    "followed by Accept" should {
      "result in merged Accept" in {
        Accept.flatMap(_ => Accept) should be (AcceptC(Seq(Event, Event)))
      }
    }

    "followed by Reject" should {
      "result in Reject" in {
        Accept.flatMap(_ => Reject) should be (Reject)
      }
    }
  }

}
