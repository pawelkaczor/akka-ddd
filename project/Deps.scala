import sbt._
import Keys._

object Deps {

  val AkkaVersion = "2.3.8"
  val AkkaHttpVersion = "1.0-M2"

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
    val client = apply("eventstore-client", "1.0.1")
    // fork available here: https://github.com/pawelkaczor/EventStore.Akka.Persistence
    val akkaJournal = apply("akka-persistence-eventstore", "1.1.2-SNAPSHOT")
    private def apply(moduleName: String, ver: String) = "com.geteventstore" %% moduleName % ver withSources()
  }

  object SqlDb {
    val `slick-for-pg` = "com.github.tminglei" %% "slick-pg" % "0.7.0"
    val testDriver = "com.h2database" % "h2" % "1.3.170" % "test"

    def prod = `slick-for-pg`

    def apply() = Seq(`slick-for-pg`, testDriver)
  }
}
