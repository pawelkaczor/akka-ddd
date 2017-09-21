package pl.newicom.dddd.writefront

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.HeaderDirectives._

trait CommandDirectives {

  def commandManifest[T]: Directive1[Manifest[T]] =
    headerValueByName("Command-Type").map[Manifest[T]](ct => Manifest.classType[T](Class.forName(ct)))

  def optionalCommandId: Directive1[Option[String]] =
    optionalHeaderValueByName("Command-ID")
}

