package pl.newicom.dddd.delivery

import akka.actor.ActorPath

package object protocol {

  trait Receipt

  type DeliveryHandler = Function[(ActorPath, Any), Unit]

}
