package pl.newicom.dddd.streams

import akka.actor.Actor
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings

trait ImplicitMaterializer { this: Actor ⇒

  def materializerSettings: ActorMaterializerSettings = ActorMaterializerSettings(context.system)

  final implicit val materializer: ActorMaterializer = ActorMaterializer(Some(materializerSettings))
}
