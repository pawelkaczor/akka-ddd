package pl.newicom.dddd.aggregate

import pl.newicom.dddd.actor.PassivationConfig

trait Config {
  def pc: PassivationConfig
}

case class DefaultConfig(pc: PassivationConfig) extends Config

trait ConfigClass[CC <: Config] {
  this: AggregateRootBase =>
  type C = CC
}