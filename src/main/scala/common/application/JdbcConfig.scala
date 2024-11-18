package es.eriktorr
package common.application

import common.Secret
import common.application.JdbcConfig.{ConnectUrl, DriverClassName, Password, Username}
import common.data.refined.Constraints.{JdbcUrl, NonEmptyString}

import cats.Show
import cats.collections.Range
import io.github.iltotore.iron.*

final case class JdbcConfig(
    connections: Range[Int],
    connectUrl: ConnectUrl,
    driverClassName: DriverClassName,
    password: Secret[Password],
    username: Username,
)

object JdbcConfig:
  opaque type ConnectUrl <: String :| JdbcUrl = String :| JdbcUrl
  object ConnectUrl extends RefinedTypeOps[String, JdbcUrl, ConnectUrl]

  opaque type DriverClassName = String :| NonEmptyString
  object DriverClassName extends RefinedTypeOps[String, NonEmptyString, DriverClassName]

  opaque type Password <: String :| NonEmptyString = String :| NonEmptyString
  object Password extends RefinedTypeOps[String, NonEmptyString, Password]:
    given Show[Password] = Show.fromToString

  opaque type Username <: String :| NonEmptyString = String :| NonEmptyString
  object Username extends RefinedTypeOps[String, NonEmptyString, Username]

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
