package pl.newicom.dddd.process

import pl.newicom.dddd.actor.{Config, PassivationConfig}
import pl.newicom.dddd.office.OfficeId

abstract class ProcessManager[S <: SagaState[S], P <: ProcessManager[S, P] : ProcessConfig] extends Saga with SagaCollaboration with SagaStateHandling[S] {
  def processManagerId: String = sagaId

  def config: Config

  override def pc: PassivationConfig = config.pc

  override def officeId: OfficeId = implicitly[ProcessConfig[P]]

}