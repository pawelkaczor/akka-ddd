package pl.newicom.eventstore

import eventstore.EventStream.{Plain, System}
import eventstore.{EventStream => ESEventStream}
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.office.{LocalOfficeId, RemoteOfficeId, SagaConfig}

object StreamNameResolver {

  def streamId(observable: BusinessEntity): ESEventStream.Id = observable match {

    case o: SagaConfig[_] =>
      Plain(s"${o.id}")

    case LocalOfficeId(id) =>
      System(s"ce-$id")

    case RemoteOfficeId(id) =>
      System(s"ce-$id")

    case clerk: BusinessEntity =>
      Plain(s"${clerk.id}")
  }
}
