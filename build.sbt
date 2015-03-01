import Deps._
import sbt.Keys._
import sbtrelease.ReleasePlugin._

name := "akka-ddd"

version in ThisBuild := "1.0.2-SNAPSHOT"

organization in ThisBuild := "pl.newicom.dddd"

scalaVersion in ThisBuild := "2.11.5"

scalacOptions in ThisBuild := Seq("-encoding", "utf8", "-feature", "-language:postfixOps")

publishMavenStyle in ThisBuild := true

lazy val root = (project in file("."))
  .settings(`Pub&RelSettings`: _*)
  .aggregate(`akka-ddd-messaging`, `akka-ddd-core`, `akka-ddd-write-front`, `view-update`, `view-update-sql`, `akka-ddd-test`, `eventstore-akka-persistence`, `http-support`)
  .settings(
    publishArtifact := false)

lazy val `akka-ddd-messaging` = project
  .settings(`Pub&RelSettings`: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    libraryDependencies ++= Json.`4s` ++ Seq(
      Akka.actor,
      "com.github.nscala-time" %% "nscala-time" % "1.4.0"
    ),
    startYear := Some(2014)
  )

lazy val `akka-ddd-core` = project
  .settings(`Pub&RelSettings`: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    startYear := Some(2014),
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      Akka.actor, Akka.contrib, Akka.persistence, Akka.slf4j
    ))
  .dependsOn(`akka-ddd-messaging`)

lazy val `akka-ddd-write-front` = project
  .settings(`Pub&RelSettings`: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    startYear := Some(2014),
    publishArtifact in Test := true,
    libraryDependencies ++= Json.`4s` ++ Akka.http ++ Seq(
      Akka.contrib
    ))
  .dependsOn(`akka-ddd-messaging`)

lazy val `view-update` = project
  .settings(`Pub&RelSettings`: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    startYear := Some(2014))
  .dependsOn(`akka-ddd-messaging`, `eventstore-akka-persistence`)

lazy val `view-update-sql` = project
  .settings(`Pub&RelSettings`: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    libraryDependencies ++= Seq(
      SqlDb.prod
    ),
    startYear := Some(2014))
  .dependsOn(`view-update`)

lazy val `akka-ddd-test` = project
  .settings(`Pub&RelSettings`: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    libraryDependencies ++= Seq(
      Akka.actor, Akka.contrib, Akka.persistence, Akka.slf4j,
      Akka.testkit, Akka.multiNodeTestkit,
      "org.scalacheck" %% "scalacheck" % "1.11.6",
      "org.scalatest" %% "scalatest" % "2.2.4",
      "commons-io" % "commons-io" % "2.4"
    ),
    startYear := Some(2014))
  .dependsOn(`akka-ddd-core`)

lazy val `eventstore-akka-persistence` = project
  .settings(`Pub&RelSettings`: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    libraryDependencies ++= Seq(
      Eventstore.client excludeAll ExclusionRule(organization = "com.typesafe.akka"),
      Eventstore.akkaJournal excludeAll ExclusionRule(organization = "com.typesafe.akka"),
      Json4s.native, Json4s.ext,
      Akka.slf4j, Akka.persistence
    ),
    startYear := Some(2014))
  .dependsOn(`akka-ddd-messaging`)

lazy val `http-support` = project
  .settings(`Pub&RelSettings`: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    libraryDependencies ++= Json.`4s` ++ Akka.http,
    startYear := Some(2014)
  )

lazy val `Pub&RelSettings`: Seq[Def.Setting[_]] = Publish.settings ++ releaseSettings

