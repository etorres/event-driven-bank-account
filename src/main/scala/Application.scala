package es.eriktorr

import accounts.service.{AccountServiceFacade, AccountStatementPrinter}
import application.AppConfig
import common.db.JdbcTransactor

import cats.effect.{ExitCode, IO}
import cats.implicits.showInterpolator
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Application extends CommandIOApp(name = "bank-account", header = "Bank Account"):
  override def main: Opts[IO[ExitCode]] = AppConfig.opts.map(program)

  private def program(config: AppConfig) = for
    logger <- Slf4jLogger.create[IO]
    _ <- logger.info(show"Starting application with configuration: $config")
    _ <- (for
      transactor <- JdbcTransactor(config.backendJdbcConfig).transactorResource
      accountStatementPrinter = AccountStatementPrinter.impl
      accountServiceFacade <- AccountServiceFacade.impl(accountStatementPrinter, transactor)
    yield accountServiceFacade).use: accountServiceFacade =>
      accountServiceFacade.createAccount.flatMap(IO.println) // TODO
  yield ExitCode.Success
