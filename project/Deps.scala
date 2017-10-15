import sbt._

object Deps {

  object Version {
    val Akka              = "2.5.6"
    val AkkaContribLegacy = "0.1.0"
    val AkkaHttp          = "10.0.10"

    val Enumeratum            = "1.5.12"
    val EventStoreClient      = "5.0.1"
    val EventStoreAkkaJournal = "5.0.1"

    val json4s = "3.5.3"

    val Slick              = "3.2.1"
    val PostgresqlSlickExt = "0.15.3" // Slick 3.2.1
    val H2Driver           = "1.4.189"

    // monitoring
    val Kamon = "0.6.6"

    // test
    val ScalaTest  = "3.0.1"
    val ScalaCheck = "1.13.4"

    val LogbackClassic = "1.1.7"
    val nScalaTime     = "2.16.0"
  }

  object Akka {
    val actor            = apply("actor")
    val clusterTools     = apply("cluster-tools")
    val clusterSharding  = apply("cluster-sharding")
    val contributions    = "pl.newicom" %% "akka-contrib-legacy" % Version.AkkaContribLegacy
    val multiNodeTestkit = apply("multi-node-testkit")
    val persistence      = apply("persistence")
    val persistenceQuery = apply("persistence-query")
    val slf4j            = apply("slf4j")
    val stream           = apply("stream")
    val testkit          = apply("testkit")

    private def apply(moduleName: String) = "com.typesafe.akka" %% s"akka-$moduleName" % Version.Akka
  }

  object AkkaHttp {
    val httpCore    = apply("http")
    val httpTestKit = apply("http-testkit")
    val all         = Seq(httpCore, httpTestKit)

    private def apply(moduleName: String) = "com.typesafe.akka" %% s"akka-$moduleName" % Version.AkkaHttp
  }

  object Json {
    val `4s` = Seq(Json4s.native, Json4s.ext, Enum.jsonSupport)
  }

  object Enum {
    val enumeratum  = "com.beachape" %% "enumeratum"        % Version.Enumeratum
    val jsonSupport = "com.beachape" %% "enumeratum-json4s" % Version.Enumeratum
  }

  object Json4s {
    val native = apply("native")
    val ext    = apply("ext")

    private def apply(moduleName: String) = "org.json4s" %% s"json4s-$moduleName" % Version.json4s
  }

  object Eventstore {
    val client      = "com.geteventstore" %% "eventstore-client"           % Version.EventStoreClient
    val akkaJournal = "com.geteventstore" %% "akka-persistence-eventstore" % Version.EventStoreAkkaJournal
  }

  object SqlDb {
    val `slick-for-pg` = "com.github.tminglei" %% "slick-pg"       % Version.PostgresqlSlickExt exclude ("org.slf4j", "slf4j-simple")
    val connectionPool = "com.typesafe.slick"  %% "slick-hikaricp" % Version.Slick
    val testDriver     = "com.h2database"      % "h2"              % Version.H2Driver % "test"

    def apply() = Seq(`slick-for-pg`, connectionPool, testDriver)
  }

  object Kamon {
    val core                     = apply("core")
    private def apply(m: String) = "io.kamon" %% s"kamon-$m" % Version.Kamon
  }

  object TestFrameworks {
    val scalaTest  = "org.scalatest"  %% "scalatest"  % Version.ScalaTest
    val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.ScalaCheck
  }

  val levelDB        = Seq("org.iq80.leveldb" % "leveldb" % "0.7", "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8")
  val commonIO       = "commons-io" % "commons-io" % "2.4"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % Version.LogbackClassic
  val nscalaTime     = "com.github.nscala-time" %% "nscala-time" % Version.nScalaTime
}
