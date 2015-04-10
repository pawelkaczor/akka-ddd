package pl.newicom.dddd.clustertest

import java.io.File

import akka.actor.{ActorIdentity, Identify, Props}
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import pl.newicom.dddd.clustertest.MultiNodeWithLevelDbJournalSpec.LevelDbJournalConfig

object MultiNodeWithLevelDbJournalSpec {
  val LevelDbJournalConfig = ConfigFactory.parseString("""
    akka.persistence.journal.plugin = "akka.persistence.journal.leveldb-shared"
    akka.persistence.snapshot-store.plugin = "eventstore.persistence.snapshot-store"
    akka.persistence.journal.leveldb-shared.store {
      native = off
      dir = "target/test-shared-journal"
    }
    akka.persistence.snapshot-store.local.dir = "target/test-snapshots"
                            """)
}

abstract class MultiNodeWithLevelDbJournalSpec extends SimpleClusterSpec(LevelDbJournalConfig) {

  val storageLocations = List(
    "akka.persistence.journal.leveldb.dir",
    "akka.persistence.journal.leveldb-shared.store.dir",
    "akka.persistence.snapshot-store.local.dir").map(s => new File(system.settings.config.getString(s)))

  override protected def atStartup() {
    runOn(firstNode) {
      storageLocations.foreach(dir => FileUtils.deleteDirectory(dir))
    }
  }

  override protected def afterTermination() {
    runOn(firstNode) {
      storageLocations.foreach(dir => FileUtils.deleteDirectory(dir))
    }
  }

  def setupSharedJournal(barier: String = "after-1") {
    Persistence(system)
    runOn(firstNode) {
      system.actorOf(Props[SharedLeveldbStore], "store")
    }
    enterBarrier("persistence-started")

    system.actorSelection(node(firstNode) / "user" / "store") ! Identify(None)
    val sharedStore = expectMsgType[ActorIdentity].ref.get
    SharedLeveldbJournal.setStore(sharedStore, system)

    enterBarrier(barier)
  }
  

}
