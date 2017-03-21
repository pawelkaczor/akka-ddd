import Deps._
import Deps.TestFrameworks._
import sbt.Keys._
import java.net.URL

name := "akka-ddd"

version      in ThisBuild := "1.5.2-SNAPSHOT"
organization in ThisBuild := "pl.newicom.dddd"
scalaVersion in ThisBuild := "2.12.1"
crossScalaVersions in ThisBuild := Seq("2.12.1", "2.11.8")

scalacOptions     in ThisBuild := Seq("-encoding", "utf8", "-deprecation", "-feature", "-language:postfixOps", "-unchecked")
publishMavenStyle in ThisBuild := true
homepage          in ThisBuild := Some(new URL("http://github.com/pawelkaczor/akka-ddd"))
licenses          in ThisBuild := ("Apache2", new URL("http://raw.githubusercontent.com/pawelkaczor/akka-ddd/master/LICENSE.md")) :: Nil

lazy val root = (project in file("."))
  .aggregate(`akka-ddd-messaging`, `akka-ddd-monitoring`, `akka-ddd-core`, `akka-ddd-write-front`, `view-update`, `view-update-sql`, `akka-ddd-test`, `eventstore-akka-persistence`, `http-support`, `akka-ddd-scheduling`)
  .settings(
    commonSettings,
    publishArtifact := false
  )


lazy val `akka-ddd-messaging` = project
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:implicitConversions"),
    libraryDependencies ++= Json.`4s` ++ Seq(Akka.stream, nscalaTime)
  )


lazy val `akka-ddd-core` = project
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:implicitConversions"),
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      Akka.clusterTools, Akka.clusterSharding, Akka.persistence, Akka.contributions, Akka.slf4j
    ))
  .dependsOn(`akka-ddd-messaging`)


lazy val `akka-ddd-write-front` = project
  .settings(
    commonSettings,
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      Akka.clusterTools, Akka.slf4j
    ))
  .dependsOn(`http-support`)


lazy val `view-update` = project
  .settings(
    commonSettings
  ).dependsOn(`akka-ddd-messaging`)


lazy val `view-update-sql` = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:existentials", "-language:implicitConversions"),
    inConfig(IntegrationTest)(Defaults.testTasks),
    testOptions       in Test            := Seq(Tests.Filter(specFilter)),
    testOptions       in IntegrationTest := Seq(Tests.Filter(integrationFilter)),
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= SqlDb() ++ Seq(
      scalaTest % "test", Akka.testkit % "test",
      logbackClassic % "test", scalaCheck % "test"

    ))
  .dependsOn(`view-update`, `akka-ddd-test` % "test->compile;test->test", `eventstore-akka-persistence` % "test->compile")


lazy val `akka-ddd-test` = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:implicitConversions"),
    inConfig(IntegrationTest)(Defaults.testTasks),
    testOptions       in Test            := Seq(Tests.Filter(specFilter)),
    testOptions       in IntegrationTest := Seq(Tests.Filter(integrationFilter)),
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= levelDB ++ Seq(
      Akka.testkit, Akka.multiNodeTestkit, scalaCheck, scalaTest, commonIO, logbackClassic % "test"
    ))
  .dependsOn(`akka-ddd-core`, `eventstore-akka-persistence` % "test->compile")


lazy val `eventstore-akka-persistence` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Json.`4s` ++ Seq(
      Eventstore.client, Eventstore.akkaJournal,
      Akka.slf4j, Akka.persistence
    ))
  .dependsOn(`akka-ddd-messaging`)


lazy val `http-support` = project
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:implicitConversions"),
    libraryDependencies ++= AkkaHttp.all
  ).dependsOn(`akka-ddd-messaging`)


lazy val `akka-ddd-scheduling` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Akka.testkit % "test", scalaCheck % "test",
      logbackClassic % "test"
    ))
  .dependsOn(`akka-ddd-core`, `eventstore-akka-persistence`, `akka-ddd-test` % "test->compile;test->test")


lazy val `akka-ddd-monitoring` = project
  .settings(
    commonSettings,
    libraryDependencies += Kamon.core
  ).dependsOn(`akka-ddd-core`)

lazy val commonSettings: Seq[Setting[_]] = Publish.settings ++ Seq(
  updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
  licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
  startYear := Some(2014)
)


lazy val IntegrationTest = config("it") extend Test

def integrationFilter(name: String): Boolean = name endsWith "IntegrationSpec"
def specFilter(name: String): Boolean = (name endsWith "Spec") && !integrationFilter(name)
