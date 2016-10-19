package pl.newicom.dddd.coordination

import akka.actor.ActorPath
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.coordination.ReceptorConfig.{ReceiverResolver, StimuliSource, Transduction}
import pl.newicom.dddd.messaging.Message
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.LocalOfficeId

object ReceptorConfig {
  type Transduction = PartialFunction[EventMessage, Message]
  type ReceiverResolver = PartialFunction[Message, ActorPath]
  type StimuliSource = BusinessEntity
}


case class ReceptorConfig(
                           stimuliSource: StimuliSource,
                           transduction: Transduction,
                           receiverResolver: ReceiverResolver,
                           capacity: Int,
                           isSupporting_MustFollow_Attribute: Boolean = true
)


trait ReceptorGrammar {
  def reactTo[A : LocalOfficeId]:                                     ReceptorGrammar
  def applyTransduction(transduction: Transduction):                  ReceptorGrammar
  def route(receiverResolver: ReceiverResolver):                      ReceptorConfig
  def propagateTo(receiver: ActorPath):                               ReceptorConfig
}


case class ReceptorBuilder(
                            stimuliSource:    StimuliSource = null,
                            transduction:     Transduction = {case em => em},
                            receiverResolver: ReceiverResolver = null,
                            capacity:         Int = 1000)
  extends ReceptorGrammar {

  def reactTo[A : LocalOfficeId]: ReceptorBuilder = {
    reactTo(implicitly[LocalOfficeId[A]].asInstanceOf[BusinessEntity])
  }

  def reactTo(observable: BusinessEntity): ReceptorBuilder = {
    copy(stimuliSource = observable)
  }

  def applyTransduction(transduction: Transduction): ReceptorBuilder =
    copy(transduction = transduction)

  def route(receiverResolver: ReceiverResolver): ReceptorConfig =
    ReceptorConfig(stimuliSource, transduction, receiverResolver, capacity)

  def propagateTo(receiver: ActorPath): ReceptorConfig =
    route({case _ => receiver})

  def withCapacity(capacity: Int): ReceptorBuilder =
    copy(capacity = capacity)
}