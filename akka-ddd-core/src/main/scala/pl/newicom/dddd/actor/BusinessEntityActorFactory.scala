package pl.newicom.dddd.actor

import akka.actor.Props
import pl.newicom.dddd.aggregate.BusinessEntity

import scala.annotation.implicitNotFound
import scala.concurrent.duration.Duration

@implicitNotFound("could not find factory for ${A} \nsee: BusinessEntityActorFactory")
abstract class BusinessEntityActorFactory[A <: BusinessEntity] {
  def props(pc: PassivationConfig): Props
  def inactivityTimeout: Duration
}
