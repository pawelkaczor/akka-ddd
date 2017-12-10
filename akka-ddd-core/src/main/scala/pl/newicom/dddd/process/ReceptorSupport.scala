package pl.newicom.dddd.process

import akka.actor.{ActorRef, ActorSystem}
import pl.newicom.dddd.coordination.ReceptorConfig
import pl.newicom.dddd.office.{LocalOfficeId, OfficeRegistry}

object ReceptorSupport {

  def receptor[A : ReceptorActorFactory](config: ReceptorConfig)(implicit as: ActorSystem): ActorRef =
    implicitly[ReceptorActorFactory[A]].apply(config)
}

object CommandReceptorSupport {

  case class CommandReception(department: String) {
    implicit def commandReceptionOfficeId: LocalOfficeId[CommandReception] =
      new LocalOfficeId[CommandReception]("command-reception", department)

    def receptor[A <: CommandReception : ReceptorActorFactory](implicit as: ActorSystem): ActorRef =
      ReceptorSupport.receptor[A](CommandQueueReceptor(department)(OfficeRegistry(as)))

  }

}