import Deps._
import sbt.Keys._
import sbtrelease.ReleasePlugin._

name := "akka-ddd"

version in ThisBuild := "1.0.0-SNAPSHOT"

organization in ThisBuild := "pl.newicom.dddd"

scalaVersion in ThisBuild := "2.11.2"

scalacOptions in ThisBuild := Seq("-encoding", "utf8", "-feature", "-language:postfixOps")

publishMavenStyle in ThisBuild := true

lazy val root = (project in file("."))
  .aggregate(`akka-ddd-messaging`, `akka-ddd-core`, `akka-ddd-view`)
  .settings(
    packagedArtifacts := Map.empty)

lazy val `akka-ddd-messaging` = project
  .settings(Publish.settings ++ releaseSettings: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    startYear := Some(2014)
  )

lazy val `akka-ddd-core` = project
  .settings(Publish.settings ++ releaseSettings: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    startYear := Some(2014),
    publishArtifact in (Test, packageBin) := true,
    libraryDependencies ++= Seq(
      Akka.actor, Akka.contrib, Akka.persistence, Akka.slf4j,
      Akka.testkit, Akka.multiNodeTestkit,
      "org.scalacheck" %% "scalacheck" % "1.11.6" % "test",
      "org.scalatest" %% "scalatest" % "2.1.6" % "test",
      "commons-io" % "commons-io" % "2.4" % "test",
      "pl.project13.scala" %% "rainbow" % "0.2" % "test"
    ))
  .dependsOn(`akka-ddd-messaging`)


lazy val `akka-ddd-view` = project
  .settings(Publish.settings ++ releaseSettings: _*)
  .settings(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    libraryDependencies ++= Seq(
      Akka.actor
    ),
    startYear := Some(2014))
  .dependsOn(`akka-ddd-messaging`)



