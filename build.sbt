import Deps._
import Deps.TestFrameworks._
import sbt.Keys._
import java.net.URL

name := "akka-ddd"

version      in ThisBuild := "1.6.0-SNAPSHOT"
organization in ThisBuild := "pl.newicom.dddd"
scalaVersion in ThisBuild := "2.12.2"
crossScalaVersions in ThisBuild := Seq("2.12.2", "2.11.11")

scalacOptions     in ThisBuild := Seq("-encoding", "utf8", "-deprecation", "-feature", "-language:postfixOps", "-language:implicitConversions", "-unchecked")

publishMavenStyle in ThisBuild := true
homepage          in ThisBuild := Some(new URL("http://github.com/pawelkaczor/akka-ddd"))
licenses          in ThisBuild := ("Apache2", new URL("http://raw.githubusercontent.com/pawelkaczor/akka-ddd/master/LICENSE.md")) :: Nil

lazy val root = (project in file("."))
  .aggregate(`akka-ddd-protocol`, `akka-ddd-messaging`, `akka-ddd-monitoring`, `akka-ddd-core`, `akka-ddd-write-front`, `view-update`, `view-update-sql`, `akka-ddd-test`, `eventstore-akka-persistence`, `http-support`, `akka-ddd-scheduling`)
  .settings(
    commonSettings,
    publishArtifact := false
  )


lazy val `akka-ddd-protocol` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(Akka.actor) ++ Json.`4s`
  )

lazy val `akka-ddd-messaging` = project
  .settings(
    libraryDependencies ++= Seq(Akka.stream, nscalaTime)
  ).dependsOn(`akka-ddd-protocol`)


lazy val `akka-ddd-core` = project
  .settings(
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
  .dependsOn(`akka-ddd-messaging`)


lazy val `view-update-sql` = project
  .configs(IntegrationTest)
  .settings(
    scalacOptions ++= Seq("-language:existentials"),
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
    libraryDependencies ++= Seq(
      Eventstore.client, Eventstore.akkaJournal,
      Akka.slf4j, Akka.persistence
    ))
  .dependsOn(`akka-ddd-messaging`)


lazy val `http-support` = project
  .settings(
    commonSettings,
    libraryDependencies ++= AkkaHttp.all
  ).dependsOn(`akka-ddd-protocol`)


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
