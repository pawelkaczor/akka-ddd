import Deps._
import Deps.TestFrameworks._
import sbt.Keys._
import sbtrelease.ReleasePlugin._
import java.net.URL

name := "akka-ddd"

version in ThisBuild := "1.0.4-M2"
organization in ThisBuild := "pl.newicom.dddd"
scalaVersion in ThisBuild := "2.11.6"

scalacOptions     in ThisBuild := Seq("-encoding", "utf8", "-feature", "-language:postfixOps")
publishMavenStyle in ThisBuild := true
homepage          in ThisBuild := Some(new URL("http://github.com/pawelkaczor/akka-ddd"))
licenses          in ThisBuild := ("Apache2", new URL("http://raw.githubusercontent.com/pawelkaczor/akka-ddd/master/LICENSE.md")) :: Nil

lazy val root = (project in file("."))
  .aggregate(`akka-ddd-messaging`, `akka-ddd-core`, `akka-ddd-write-front`, `view-update`, `view-update-sql`, `akka-ddd-test`, `eventstore-akka-persistence`, `http-support`, `akka-ddd-scheduling`)
  .settings(
    commonSettings,
    publishArtifact := false
  )

lazy val `akka-ddd-messaging` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Json.`4s` ++ Seq(
      Akka.actor,
      "com.github.nscala-time" %% "nscala-time" % "2.0.0"
    )
  )

lazy val `akka-ddd-core` = project
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:implicitConversions"),
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      Akka.clusterTools, Akka.clusterSharding, Akka.persistence, Akka.slf4j
    ))
  .dependsOn(`akka-ddd-messaging`)

lazy val `akka-ddd-write-front` = project
  .settings(
    commonSettings,
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      Akka.clusterTools
    ))
  .dependsOn(`akka-ddd-messaging`, `http-support`)

lazy val `view-update` = project
  .settings(
    commonSettings
  ).dependsOn(`akka-ddd-messaging`, `eventstore-akka-persistence`)

lazy val `view-update-sql` = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:existentials"),
    inConfig(IntegrationTest)(Defaults.testTasks),
    testOptions       in Test            := Seq(Tests.Filter(specFilter)),
    testOptions       in IntegrationTest := Seq(Tests.Filter(integrationFilter)),
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= Seq(
      SqlDb.prod, scalaTest % "test", SqlDb.testDriver, Akka.testkit % "test",
      "ch.qos.logback" % "logback-classic" % "1.1.2" % "test", scalaCheck % "test"

    ))
  .dependsOn(`view-update`, `akka-ddd-test` % "test->compile;test->test")

lazy val `akka-ddd-test` = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:implicitConversions"),
    inConfig(IntegrationTest)(Defaults.testTasks),
    testOptions       in Test            := Seq(Tests.Filter(specFilter)),
    testOptions       in IntegrationTest := Seq(Tests.Filter(integrationFilter)),
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= Seq(
      Akka.testkit, Akka.multiNodeTestkit, scalaCheck, scalaTest,
      "org.iq80.leveldb"            % "leveldb"          % "0.7",
      "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.7",
      "commons-io" % "commons-io" % "2.4"
    ))
  .dependsOn(`akka-ddd-core`, `eventstore-akka-persistence` % "test->compile")

lazy val `eventstore-akka-persistence` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Eventstore.client, Eventstore.akkaJournal,
      Json4s.native, Json4s.ext,
      Akka.slf4j, Akka.persistence
    ))
  .dependsOn(`akka-ddd-messaging`)

lazy val `http-support` = project
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-language:implicitConversions"),
    libraryDependencies ++= Json.`4s` ++ Akka.http
  )

lazy val `akka-ddd-scheduling` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Akka.testkit % "test", scalaCheck % "test",
      "ch.qos.logback" % "logback-classic" % "1.1.2" % "test"
    ))
  .dependsOn(`akka-ddd-core`, `eventstore-akka-persistence`, `akka-ddd-test` % "test->compile;test->test")

lazy val commonSettings: Seq[Setting[_]] = Publish.settings ++ releaseSettings ++ Seq(
  updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
  licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
  startYear := Some(2014)
)

lazy val IntegrationTest = config("it") extend Test

def integrationFilter(name: String): Boolean = name endsWith "IntegrationSpec"
def specFilter(name: String): Boolean = (name endsWith "Spec") && !integrationFilter(name)
