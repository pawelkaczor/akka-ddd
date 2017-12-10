package pl.newicom.dddd.office

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.cluster
import java.util.concurrent.ConcurrentHashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

class OfficeRegistryImpl(implicit as: ActorSystem) extends Extension {

  val _inClusterOffices = new ConcurrentHashMap[EntityId, OfficeRef]
  val _externalOffices  = new ConcurrentHashMap[EntityId, OfficeId]

  lazy val log: Logger = getLogger(getClass.getName)

  def registerOffice(officeRef: OfficeRef): Unit = {
    val id = officeRef.officeId.id
    _inClusterOffices.put(id, officeRef)
    log.debug("In-cluster office registered: {}", id)
  }

  def registerOffice(officeId: RemoteOfficeId[_], external: Boolean): Unit =
    if (external) {
      _externalOffices.put(officeId.id, officeId)
      log.debug("External office registered: {}", officeId.id)
    } else {
      officeRef(officeId)
    }

  def isOfficeAvailableInCluster(officeId: EntityId): Boolean =
    _inClusterOffices.containsKey(officeId)

  def isOfficeRegistered(officeId: EntityId): Boolean =
    _externalOffices.containsKey(officeId) || _inClusterOffices.containsKey(officeId)

  def officeRef(officeId: RemoteOfficeId[_]): OfficeRef = {
    if (!isOfficeAvailableInCluster(officeId.id)) {
      registerOffice(new OfficeRef(officeId, cluster.proxy(officeId)))
    }
    officeRef(officeId.id)
  }

  def officeRef(officeId: EntityId): OfficeRef = {
    val ref = _inClusterOffices.get(officeId)
    if (ref == null) {
      throw new RuntimeException(s"Unknown office: $officeId")
    }
    ref
  }

  def find(p: OfficeId => Boolean): Option[OfficeId] =
    offices.find(p)

  private def offices: Set[OfficeId] =
  _inClusterOffices.values().asScala.toSet.map((o: OfficeRef) => o.officeId) ++
    _externalOffices.values().asScala.toSet

}

object OfficeRegistry extends ExtensionId[OfficeRegistryImpl] with ExtensionIdProvider {
  override def lookup(): ExtensionId[OfficeRegistryImpl] = OfficeRegistry

  override def createExtension(system: ExtendedActorSystem): OfficeRegistryImpl =
    new OfficeRegistryImpl()(system)

}
