package pl.newicom.dddd.saga

import akka.actor.ActorRef
import pl.newicom.dddd.office.Office

import scala.reflect.ClassTag

class SagaOffice[E: ClassTag](val config: SagaConfig[E], actor: ActorRef) extends Office[E](config, actor) {
  def businessProcess = BusinessProcess(config.bpsName, config.department)
}
