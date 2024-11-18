package es.eriktorr
package common.db

import common.application.JdbcTestConfig

import cats.effect.{IO, Resource}
import cats.implicits.toFoldableOps
import doobie.Fragment
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class PostgresTestTransactor(jdbcTestConfig: JdbcTestConfig):
  val testTransactorResource: Resource[IO, HikariTransactor[IO]] = for
    transactor <- JdbcTransactor(jdbcTestConfig.config).transactorResource
    _ <- Resource.eval((for
      tableNames <-
        sql"""SELECT table_name
             |FROM information_schema.tables
             |WHERE table_schema='public'
             | AND table_type='BASE TABLE'
             | AND table_name NOT LIKE 'flyway_%'""".stripMargin.query[String].to[List]
      _ <- tableNames
        .map(tableName => Fragment.const(s"truncate table $tableName cascade"))
        .traverse_(_.update.run)
    yield ()).transact(transactor))
  yield transactor
