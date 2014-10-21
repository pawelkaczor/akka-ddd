import sbt._
import Keys._
import xerial.sbt.Sonatype.sonatypeSettings

object Publish {
  lazy val settings = sonatypeSettings :+ (pomExtra :=
    <scm>
      <url>git@github.com:pawelkaczor/akka-ddd.git</url>
      <connection>scm:git:git@github.com:pawelkaczor/akka-ddd.git</connection>
      <developerConnection>scm:git:git@github.com:pawelkaczor/akka-ddd.git</developerConnection>
    </scm>
      <developers>
        <developer>
          <id>newicom</id>
          <name>Pawel Kaczor</name>
          <url>http://pkaczor.blogspot.com</url>
        </developer>
      </developers>)
}