package pl.newicom.dddd.aggregate

import pl.newicom.dddd.actor.PassivationConfig

trait Config {
  def pc: PassivationConfig
  def respondingPolicy: RespondingPolicy
}

case class DefaultConfig(pc: PassivationConfig, replyWithEvents: Boolean = true) extends Config {
  def respondingPolicy: RespondingPolicy =
    if (replyWithEvents) ReplyWithEvents else SparseReply
}

trait ConfigClass[CC <: Config] {
  this: AggregateRootBase =>
  type C = CC
}
