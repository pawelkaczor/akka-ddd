import sbt.Keys._

name := "akka-ddd"

organization := "pl.newicom"

scalaVersion := "2.11.2"

scalacOptions := Seq("-encoding", "utf8", "-feature", "-language:postfixOps")

val akkaVersion = "2.3.6"

lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

val project = Project(
  id = "akka-ddd",
  base = file("."),
  settings = Seq(
    libraryDependencies ++= akka ++ Seq(
      "org.scalacheck" %% "scalacheck" % "1.11.6" % "test",
      "org.scalatest" %% "scalatest" % "2.1.6" % "test",
      "pl.project13.scala" %% "rainbow" % "0.2" % "test"
    )
  )
)



