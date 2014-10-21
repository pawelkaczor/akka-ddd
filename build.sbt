import sbt.Keys._
import sbtrelease.ReleasePlugin._

name := "akka-ddd"

organization := "pl.newicom.dddd"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.2"

publishMavenStyle := true

publishArtifact in (Test, packageBin) := true

val akkaVersion = "2.3.6"

lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

val project = Project(
  id = "akka-ddd",
  base = file("."),
  settings = Publish.settings ++ releaseSettings ++ Seq(
    licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/akka-ddd/master/LICENSE.md")),
    startYear := Some(2014),
    scalacOptions := Seq("-encoding", "utf8", "-feature", "-language:postfixOps"),
    libraryDependencies ++= akka ++ Seq(
      "org.scalacheck" %% "scalacheck" % "1.11.6" % "test",
      "org.scalatest" %% "scalatest" % "2.1.6" % "test",
      "pl.project13.scala" %% "rainbow" % "0.2" % "test"
    )
  )
)



