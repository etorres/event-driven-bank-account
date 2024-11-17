package es.eriktorr
package application

import application.JdbcConfig.{ConnectUrl, Password, Username}
import application.argument.JdbcConfigArgument.{
  connectUrlArgument,
  passwordArgument,
  usernameArgument,
}
import application.argument.RangeArgument.intRangeArgument
import common.Secret

import cats.Show
import cats.collections.Range
import cats.implicits.{catsSyntaxTuple4Semigroupal, showInterpolator}
import com.monovore.decline.Opts

final case class AppConfig(backendJdbcConfig: JdbcConfig)

object AppConfig:
  given Show[AppConfig] = Show.show(config => show"[backend: ${config.backendJdbcConfig}]")

  def opts: Opts[AppConfig] =
    val backendJdbcConfig =
      (
        Opts
          .env[Range[Int]](
            name = "BANK_ACCOUNT_BACKEND_JDBC_CONNECTIONS",
            help = "Set JDBC Connections.",
          )
          .validate("Must be between 1 and 16")(_.overlaps(Range(1, 16)))
          .withDefault(Range(1, 3)),
        Opts.env[ConnectUrl](
          name = "BANK_ACCOUNT_BACKEND_JDBC_CONNECT_URL",
          help = "Set JDBC Connect URL.",
        ),
        Opts
          .env[Password](
            name = "BANK_ACCOUNT_BACKEND_JDBC_PASSWORD",
            help = "Set JDBC Password.",
          )
          .map(Secret.apply[Password]),
        Opts.env[Username](
          name = "BANK_ACCOUNT_BACKEND_JDBC_USERNAME",
          help = "Set JDBC Username.",
        ),
      ).mapN(JdbcConfig.postgres)

    backendJdbcConfig.map(AppConfig.apply)
