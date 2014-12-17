import sbt._
import Keys._

object Deps {

  val AkkaVersion = "2.3.6"

  object Akka {
    val actor = apply("actor")
    val slf4j = apply("slf4j")
    val persistence = apply("persistence-experimental")
    val contrib = apply("contrib")
    val testkit = apply("testkit")
    val multiNodeTestkit = apply("multi-node-testkit")

    private def apply(moduleName: String) = "com.typesafe.akka" %% s"akka-$moduleName" % AkkaVersion withSources()
  }

}
