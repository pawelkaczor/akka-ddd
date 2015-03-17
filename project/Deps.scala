import sbt._
import Keys._

object Deps {

  val AkkaVersion = "2.4-SNAPSHOT"
  val AkkaHttpVersion = "1.0-M3"

  object Akka {
    val actor = apply("actor")
    val httpCore  = "com.typesafe.akka" %% "akka-http-experimental" % AkkaHttpVersion
    val httpTestKit  = "com.typesafe.akka" %% "akka-http-testkit-experimental" % AkkaHttpVersion % "test"
    val http  = Seq(httpCore, httpTestKit)
    val slf4j = apply("slf4j")
    val persistence = apply("persistence-experimental")
    val contrib = apply("contrib")
    val testkit = apply("testkit")
    val multiNodeTestkit = apply("multi-node-testkit")

    private def apply(moduleName: String) = "com.typesafe.akka" %% s"akka-$moduleName" % AkkaVersion withSources()
  }

  object Json {
    val `4s`  = Seq(Json4s.native, Json4s.ext)
  }

  object Json4s {
    val native = apply("native")
    val ext = apply("ext")

    private def apply(moduleName: String) = "org.json4s" %% s"json4s-$moduleName" % "3.2.11" withSources()
  }

  object Eventstore {
    val client = "pl.newicom.dddd" %% "eventstore-client" % "2.0.2-SNAPSHOT" withSources()
    val akkaJournal = "pl.newicom.dddd" %% "akka-persistence-eventstore" % "2.0.2-SNAPSHOT" withSources()
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
