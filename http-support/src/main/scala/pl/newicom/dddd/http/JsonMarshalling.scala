package pl.newicom.dddd.http

import akka.http.scaladsl.marshalling.{PredefinedToEntityMarshallers, ToEntityMarshaller => TEM}
import akka.http.scaladsl.model.{ContentTypeRange, MediaRange, MediaTypes}
import akka.http.scaladsl.unmarshalling.Unmarshaller.UnsupportedContentTypeException
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller => FEUM, PredefinedFromEntityUnmarshallers}
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import org.json4s.Formats
import org.json4s.native.Serialization._

import scala.concurrent.ExecutionContext

trait JsonMarshalling {

  implicit def feum[A: Manifest](implicit formats: Formats, m: Materializer, ec: ExecutionContext): FEUM[A] =
    PredefinedFromEntityUnmarshallers.stringUnmarshaller.flatMapWithInput { (entity, s) =>
      if (entity.contentType().mediaType == MediaTypes.`application/json`)
        FastFuture.successful(read[A](s))
      else
        FastFuture.failed(
          UnsupportedContentTypeException(ContentTypeRange(MediaRange(MediaTypes.`application/json`)))
        )
    }

  implicit def tem[A <: AnyRef](implicit formats: Formats): TEM[A] = {
    val stringMarshaller = PredefinedToEntityMarshallers.stringMarshaller(MediaTypes.`application/json`)
    stringMarshaller.compose(writePretty[A])
  }
}