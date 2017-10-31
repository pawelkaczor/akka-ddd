package pl.newicom.dddd.office

import akka.actor.{ActorRef, ActorSystem}
import pl.newicom.dddd.actor.BusinessEntityActorFactory
import pl.newicom.dddd.{BusinessEntity, cluster}
import pl.newicom.dddd.process.{Saga, SagaActorFactory}
import pl.newicom.dddd.saga.{CoordinationOffice, ProcessConfig}

import scala.reflect.ClassTag

object OfficeFactory {

  def office[A <: BusinessEntity : BusinessEntityActorFactory : OfficeFactory : LocalOfficeId : OfficeListener : ClassTag]: OfficeRef = {

    val officeId = implicitly[LocalOfficeId[A]]
    val actor = implicitly[OfficeFactory[A]].getOrCreate()

    val office = officeId match {
      case pc: ProcessConfig[A] =>   new CoordinationOffice[A](pc, actor)
      case LocalOfficeId(_, _)  =>   new OfficeRef(officeId, actor)
    }

    implicitly[OfficeListener[A]].officeStarted(office)
    office
  }

  def office(officeId: RemoteOfficeId[_])(implicit as: ActorSystem): OfficeRef =
    new OfficeRef(officeId, cluster.proxy(officeId))

  def coordinationOffice[A <: Saga : SagaActorFactory : OfficeFactory : ProcessConfig : OfficeListener : ClassTag]: CoordinationOffice[A] =
    office[A].asInstanceOf[CoordinationOffice[A]]
}

abstract class OfficeFactory[A <: BusinessEntity : BusinessEntityActorFactory : LocalOfficeId] {
  def officeId: LocalOfficeId[A] = implicitly[LocalOfficeId[A]]
  def getOrCreate(): ActorRef
}