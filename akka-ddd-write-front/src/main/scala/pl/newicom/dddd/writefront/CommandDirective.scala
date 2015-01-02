package pl.newicom.dddd.writefront

import akka.http.server._
import akka.http.server.directives.HeaderDirectives._

trait CommandDirective {

  def commandManifest[T]: Directive1[Manifest[T]] = {
    headerValueByName("Command-Type").map[Manifest[T]](ct => Manifest.classType[T](Class.forName(ct)))
  }
}

