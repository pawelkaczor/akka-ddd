package pl.newicom.dddd.http

import akka.http.scaladsl.server.{Directives, Route}
import org.json4s.Formats

trait EndpointConcatenation {

  implicit def enhanceEndpointWithConcatenation[A](endpoint: Endpoint[A])(implicit formats: Formats): EndpointConcatenation[A] =
    new EndpointConcatenation(endpoint)

  class EndpointConcatenation[A](endpoint: Endpoint[A])(implicit val formats: Formats) {
    def ~ (other: Endpoint[A]): Endpoint[A] = new Endpoint[A] {
      override def route(us: A): Route = {
        endpoint.route(us) ~ other.route(us)
      }
    }
  }

}

object EndpointConcatenation extends EndpointConcatenation

abstract class Endpoint[A](implicit formats: Formats) extends (A => Route) with Directives with JsonMarshalling with EndpointConcatenation {

  def apply(a: A) = route(a)

  def route(a: A): Route
}

