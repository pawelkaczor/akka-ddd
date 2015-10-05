import sbt._
import Keys._

object Deps {

  object Version {
    val Akka       = "2.4.0"
    val AkkaHttp   = "1.0"
    
    val EventStoreClient      = "2.1.1"
    val EventStoreAkkaJournal = "2.1.0"
    
    val json4s     = "3.3.0"
    
    val PostgresqlSlickExt = "0.8.2" // Slick 2.1.0
    val H2Driver           = "1.4.189"

    // test
    val ScalaTest  = "2.2.4"
    val ScalaCheck = "1.12.5"
  }

  object Akka {
    val actor             = apply("actor")
    val httpCore          = "com.typesafe.akka" %% "akka-http-experimental" % Version.AkkaHttp
    val httpTestKit       = "com.typesafe.akka" %% "akka-http-testkit-experimental" % Version.AkkaHttp % "test"
    val http              = Seq(httpCore, httpTestKit)
    val slf4j             = apply("slf4j")
    val persistence       = apply("persistence")
    val clusterTools      = apply("cluster-tools")
    val clusterSharding   = apply("cluster-sharding")
    val testkit           = apply("testkit")
    val multiNodeTestkit  = apply("multi-node-testkit")

    private def apply(moduleName: String) = "com.typesafe.akka" %% s"akka-$moduleName" % Version.Akka
  }

  object Json {
    val `4s`  = Seq(Json4s.native, Json4s.ext)
  }

  object Json4s {
    val native = apply("native")
    val ext = apply("ext")

    private def apply(moduleName: String) = "org.json4s" %% s"json4s-$moduleName" % Version.json4s
  }

  object Eventstore {
    val client = "com.geteventstore" %% "eventstore-client" % Version.EventStoreClient
    val akkaJournal = "com.geteventstore" %% "akka-persistence-eventstore" % Version.EventStoreAkkaJournal
  }

  object SqlDb {
    val `slick-for-pg` = "com.github.tminglei" %% "slick-pg" % Version.PostgresqlSlickExt exclude("org.slf4j", "slf4j-simple")
    val testDriver = "com.h2database" % "h2" % Version.H2Driver % "test"

    def prod = `slick-for-pg`

    def apply() = Seq(`slick-for-pg`, testDriver)
  }

  object TestFrameworks {
    val scalaTest = "org.scalatest" %% "scalatest" % Version.ScalaTest
    val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.ScalaCheck
  }
}
