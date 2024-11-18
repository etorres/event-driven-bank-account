package es.eriktorr
package application

import accounts.api.AccountRestController
import accounts.service.AccountServiceFacade
import common.api.{HealthService, MetricsService}

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.toSemigroupKOps
import org.http4s.dsl.io.*
import org.http4s.server.Router
import org.http4s.server.middleware.{GZip, Logger as Http4sLogger, RequestId}
import org.http4s.{HttpApp, HttpRoutes, Response, Status}
import org.typelevel.log4cats.SelfAwareStructuredLogger

import scala.annotation.tailrec
import scala.util.chaining.scalaUtilChainingOps

final class AppHttp(
    accountServiceFacade: AccountServiceFacade,
    healthService: HealthService,
    metricsService: MetricsService,
    enableLogger: Boolean = false,
)(using logger: SelfAwareStructuredLogger[IO]):
  private val maybeApiEndpoint =
    val endpoints = List(AccountRestController(accountServiceFacade))

    @tailrec
    def composedHttpRoutes(
        aggregated: HttpRoutes[IO],
        routes: List[HttpRoutes[IO]],
    ): HttpRoutes[IO] = routes match
      case Nil => aggregated
      case ::(head, next) => composedHttpRoutes(head <+> aggregated, next)

    NonEmptyList
      .fromList(endpoints.map(_.routes).collect { case Some(value) => value })
      .map(nel => composedHttpRoutes(nel.head, nel.tail))
      .map(routes =>
        metricsService
          .metricsFor(routes)
          .pipe: routes =>
            // Allow the compression of the Response body using GZip
            GZip(routes)
          .pipe: routes =>
            // Automatically generate a X-Request-ID header for a request, if one wasn't supplied
            RequestId.httpRoutes(routes)
          .pipe: routes =>
            // Log requests and responses
            if enableLogger then
              Http4sLogger.httpRoutes(
                logHeaders = true,
                logBody = true,
                redactHeadersWhen = _ => false,
                logAction = Some((msg: String) => logger.info(msg)),
              )(routes)
            else routes,
      )

  val httpApp: HttpApp[IO] =
    val livenessCheckEndpoint: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root =>
      Ok(s"${healthService.serviceName} is live")
    }

    val readinessCheckEndpoint: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root =>
      healthService.isReady.ifM(
        ifTrue = Ok(s"${healthService.serviceName} is ready"),
        ifFalse = ServiceUnavailable(s"${healthService.serviceName} is not ready"),
      )
    }

    (maybeApiEndpoint match
      case Some(apiEndpoint) =>
        Router(
          "/api/v1" -> apiEndpoint,
          healthService.livenessPath -> livenessCheckEndpoint,
          healthService.readinessPath -> readinessCheckEndpoint,
          "/" -> metricsService.prometheusExportRoutes,
        )
      case None =>
        Router(
          "/" -> HttpRoutes.of[IO] { case _ => IO.delay(Response(Status.InternalServerError)) },
        )
    ).orNotFound
