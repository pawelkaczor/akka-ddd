package pl.newicom.dddd.office

import akka.actor.{ActorRef, ActorSystem}
import pl.newicom.dddd.BusinessEntity
import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.process.{CoordinationOffice, ProcessConfig, Saga, SagaActorFactory}

import scala.reflect.ClassTag

object OfficeFactory {

  def office[A <: BusinessEntity : BusinessEntityActorFactory : OfficeFactory : LocalOfficeId : OfficeListener : ClassTag](implicit as: ActorSystem): OfficeRef = {

    val officeId = implicitly[LocalOfficeId[A]]
    val actor = implicitly[OfficeFactory[A]].getOrCreate()

    val office = officeId match {
      case pc: ProcessConfig[A]    =>   new CoordinationOffice[A](pc, actor)
      case LocalOfficeId(_, _, _)  =>   new OfficeRef(officeId, actor)
    }

    implicitly[OfficeListener[A]].officeStarted(office)
    OfficeRegistry(as).registerOffice(office)

    office
  }

  def coordinationOffice[A <: Saga : SagaActorFactory : OfficeFactory : ProcessConfig : OfficeListener : ClassTag](implicit as: ActorSystem): CoordinationOffice[A] =
    office[A].asInstanceOf[CoordinationOffice[A]]
}

abstract class OfficeFactory[A <: BusinessEntity : BusinessEntityActorFactory : LocalOfficeId] {
  def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
  def getOrCreate(): ActorRef
}