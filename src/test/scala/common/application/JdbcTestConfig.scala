package es.eriktorr
package common.application

import common.Secret
import common.application.JdbcConfig.Password.given
import common.application.JdbcConfig.{ConnectUrl, Password, Username}

import cats.collections.Range

enum JdbcTestConfig(val config: JdbcConfig, val database: String):
  case BackendDatabase
      extends JdbcTestConfig(
        JdbcConfig.postgres(
          Range(JdbcTestConfig.minConnections, JdbcTestConfig.maxConnections),
          ConnectUrl.applyUnsafe(
            s"jdbc:postgresql://${JdbcTestConfig.postgresHost}/${JdbcTestConfig.bankAccountDatabase}",
          ),
          Secret(Password.applyUnsafe(JdbcTestConfig.postgresPassword)),
          Username.applyUnsafe(JdbcTestConfig.postgresUsername),
        ),
        JdbcTestConfig.bankAccountDatabase,
      )

object JdbcTestConfig:
  final private val maxConnections = 3

  final private val minConnections = 1

  final private val postgresHost = "postgres.test:5432"

  final private val postgresPassword = "changeMe"

  final private val postgresUsername = "test"

  final private val bankAccountDatabase = "bank_account"
