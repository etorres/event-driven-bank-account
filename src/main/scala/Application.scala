package es.eriktorr

import accounts.service.{AccountServiceFacade, AccountStatementPrinter}
import application.{AppConfig, AppHttp, AppParams}
import common.api.HealthService.ServiceName
import common.api.{HealthService, MetricsService}
import common.application.HttpServer
import common.db.JdbcTransactor

import cats.effect.{ExitCode, IO, Resource}
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import org.http4s.metrics.prometheus.PrometheusExportService
import org.http4s.server.middleware.{MaxActiveRequests, Timeout}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Application extends CommandIOApp(name = "bank-account", header = "Bank Account"):
  override def main: Opts[IO[ExitCode]] = (AppConfig.opts, AppParams.opts).mapN:
    case (config, params) => program(config, params)

  private def program(config: AppConfig, params: AppParams) = for
    logger <- Slf4jLogger.create[IO]
    given SelfAwareStructuredLogger[IO] = logger
    _ <- logger.info(show"Starting application with configuration: $config")
    _ <- (for
      healthService <- HealthService.resourceWith(
        config.healthConfig,
        ServiceName.applyUnsafe("Bank account application"),
      )
      prometheusExportService <- PrometheusExportService.build[IO]
      metricsService <- MetricsService.resourceWith("http4s_server", prometheusExportService)
      transactor <- JdbcTransactor(config.jdbcConfig).transactorResource
      accountStatementPrinter = AccountStatementPrinter.impl
      accountServiceFacade <- AccountServiceFacade.impl(accountStatementPrinter, transactor)
      httpApp <- Resource.eval:
        MaxActiveRequests
          .forHttpApp[IO](config.httpServerConfig.maxActiveRequests)
          .map: middleware =>
            // Limit the number of active requests by rejecting requests over the limit defined
            middleware(
              AppHttp(accountServiceFacade, healthService, metricsService, params.verbose).httpApp,
            )
          .map: decoratedHttpApp =>
            // Limit how long the underlying service takes to respond
            Timeout.httpApp[IO](timeout = config.httpServerConfig.timeout)(decoratedHttpApp)
      _ <- HttpServer.impl(httpApp, config.httpServerConfig)
    yield healthService).use: healthService =>
      healthService.markReady.flatMap(_ => IO.never[Unit])
  yield ExitCode.Success
