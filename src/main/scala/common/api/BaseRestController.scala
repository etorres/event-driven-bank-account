package es.eriktorr
package common.api

import common.api.BaseRestController.{InvalidRequest, Transformer}
import common.data.error.HandledError
import common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import cats.effect.IO
import cats.implicits.catsSyntaxMonadError
import io.circe.Decoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request, Response}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger

abstract class BaseRestController:
  val routes: Option[HttpRoutes[IO]]

  protected def contextFrom(request: Request[IO])(using
      logger: SelfAwareStructuredLogger[IO],
  ): Throwable => IO[Response[IO]] =
    (error: Throwable) =>
      val requestId = request.headers.get(ci"X-Request-ID").map(_.head.value)
      val context = Map("http.request.id" -> requestId.getOrElse("null"))
      error match
        case invalidRequest: InvalidRequest =>
          logger.error(context, invalidRequest)("Invalid request") *> BadRequest()
        case other =>
          logger.error(context, other)(
            "Unhandled error raised while handling event",
          ) *> InternalServerError()

  protected def validatedInputFrom[A, B](
      request: Request[IO],
  )(using decoder: Decoder[A], transformer: Transformer[A, B]): IO[B] =
    request
      .as[A]
      .flatMap(transformer.transform(_).toIO)
      .adaptError:
        case error => InvalidRequest(error)

object BaseRestController:
  trait Transformer[A, B]:
    def transform(value: A): AllErrorsOr[B]

  final case class InvalidRequest(cause: Throwable)
      extends HandledError("Invalid request", Some(cause))
