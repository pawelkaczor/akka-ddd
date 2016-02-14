package pl.newicom.dddd.writefront

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.HeaderDirectives._
import org.joda.time.DateTime

trait CommandDirectives {

  def commandManifest[T]: Directive1[Manifest[T]] =
    headerValueByName("Command-Type").map[Manifest[T]](ct => Manifest.classType[T](Class.forName(ct)))

  def commandTimestamp: Directive1[DateTime] =
    provide(DateTime.now())

}

