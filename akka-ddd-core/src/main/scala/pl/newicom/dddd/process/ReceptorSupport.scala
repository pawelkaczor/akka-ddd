package pl.newicom.dddd.process

import akka.actor.{ActorRef, ActorSystem}
import pl.newicom.dddd.coordination.ReceptorConfig
import pl.newicom.dddd.office.{LocalOfficeId, OfficeRegistry}

object ReceptorSupport {

  def receptor[A : ReceptorActorFactory](config: ReceptorConfig)(implicit as: ActorSystem): ActorRef =
    implicitly[ReceptorActorFactory[A]].apply(config)
}

object CommandReceptorSupport {

  def receptor[A <: CommandReception : LocalOfficeId : ReceptorActorFactory](implicit as: ActorSystem): ActorRef =
    ReceptorSupport.receptor[A](CommandQueueReceptor(implicitly[LocalOfficeId[A]].department)(OfficeRegistry(as)))

  case class CommandReception(department: String) {
    def apply(f: LocalOfficeId[CommandReception] => ActorRef): ActorRef =
      f(new LocalOfficeId[CommandReception]("command-reception", department))
  }

}