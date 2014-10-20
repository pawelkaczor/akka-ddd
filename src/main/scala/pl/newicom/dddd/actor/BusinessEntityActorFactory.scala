package pl.newicom.dddd.actor

import akka.actor.Props
import pl.newicom.dddd.aggregate.BusinessEntity

import scala.concurrent.duration.Duration

abstract class BusinessEntityActorFactory[A <: BusinessEntity] {
  def props(pc: PassivationConfig): Props
  def inactivityTimeout: Duration
}
