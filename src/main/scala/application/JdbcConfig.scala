package es.eriktorr
package application

import application.JdbcConfig.{ConnectUrl, DriverClassName, Password, Username}
import common.Secret
import common.data.refined.Constraints.{JdbcUrl, NonEmptyString}
import common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import cats.Show
import cats.collections.Range
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

final case class JdbcConfig(
    connections: Range[Int],
    connectUrl: ConnectUrl,
    driverClassName: DriverClassName,
    password: Secret[Password],
    username: Username,
)

object JdbcConfig:
  opaque type ConnectUrl <: String :| JdbcUrl = String :| JdbcUrl
  object ConnectUrl:
    def from(value: String): AllErrorsOr[ConnectUrl] = value.refineValidatedNec[JdbcUrl]
    def unsafeFrom(value: String): ConnectUrl = from(value).orFail

  opaque type DriverClassName = String :| NonEmptyString
  object DriverClassName extends RefinedTypeOps[String, NonEmptyString, DriverClassName]

  opaque type Password <: String :| NonEmptyString = String :| NonEmptyString
  object Password:
    def from(value: String): AllErrorsOr[Password] = value.refineValidatedNec[NonEmptyString]
    def unsafeFrom(value: String): Password = from(value).orFail
    given Show[Password] = Show.fromToString

  opaque type Username <: String :| NonEmptyString = String :| NonEmptyString
  object Username:
    def from(value: String): AllErrorsOr[Username] = value.refineValidatedNec[NonEmptyString]
    def unsafeFrom(value: String): Username = from(value).orFail

  private val postgresDriverClassName: DriverClassName =
    DriverClassName.applyUnsafe("org.postgresql.Driver")

  def postgres(
      connections: Range[Int],
      connectUrl: ConnectUrl,
      password: Secret[Password],
      username: Username,
  ): JdbcConfig = JdbcConfig(
    connections,
    connectUrl,
    postgresDriverClassName,
    password,
    username,
  )

  given Show[JdbcConfig] =
    import scala.language.unsafeNulls
    Show.show(config => s"""jdbc-connections: ${config.connections.start}-${config.connections.end},
                           | jdbc-connect-url: ${config.connectUrl},
                           | jdbc-driver-class-name: ${config.driverClassName},
                           | jdbc-password: ${config.password},
                           | jdbc-username: ${config.username}""".stripMargin.replaceAll("\\R", ""))
