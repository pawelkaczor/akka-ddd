import sbt._
import Keys._

object Deps {

  val AkkaVersion = "2.4-SNAPSHOT"
  val AkkaHttpVersion = "1.0-RC2"

  object Akka {
    val actor = apply("actor")
    val httpCore  = "com.typesafe.akka" %% "akka-http-core-experimental" % AkkaHttpVersion
    val httpScala  = "com.typesafe.akka" %% "akka-http-scala-experimental" % AkkaHttpVersion
    val http  = Seq(httpCore, httpScala)
    val slf4j = apply("slf4j")
    val persistence = apply("persistence-experimental")
    val clusterTools = apply("cluster-tools")
    val clusterSharding = apply("cluster-sharding")
    val testkit = apply("testkit")
    val multiNodeTestkit = apply("multi-node-testkit")

    private def apply(moduleName: String) = "com.typesafe.akka" %% s"akka-$moduleName" % AkkaVersion
  }

  object Json {
    val `4s`  = Seq(Json4s.native, Json4s.ext)
  }

  object Json4s {
    val native = apply("native")
    val ext = apply("ext")

    private def apply(moduleName: String) = "org.json4s" %% s"json4s-$moduleName" % "3.2.11"
  }

  object Eventstore {
    val client = "pl.newicom.dddd" %% "eventstore-client" % "2.0.2-SNAPSHOT"
    val akkaJournal = "pl.newicom.dddd" %% "akka-persistence-eventstore" % "2.0.2-SNAPSHOT"
  }

  object SqlDb {
    val `slick-for-pg` = "com.github.tminglei" %% "slick-pg" % "0.8.2" exclude("org.slf4j", "slf4j-simple")
    val testDriver = "com.h2database" % "h2" % "1.3.170" % "test"

    def prod = `slick-for-pg`

    def apply() = Seq(`slick-for-pg`, testDriver)
  }

  object TestFrameworks {
    val scalaTest = "org.scalatest" %% "scalatest" % "2.2.4"
    val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.6"
  }
}
