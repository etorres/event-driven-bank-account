package es.eriktorr
package accounts.service

import accounts.service.AccountStatementPrinter.Update
import common.TemporalExtensions.{asString, dateTimeFormatter}

import cats.effect.IO
import cats.effect.std.Console

import java.time.OffsetDateTime

trait AccountStatementPrinter:
  def print(balance: BigDecimal, updates: List[Update])(using console: Console[IO]): IO[Unit]

object AccountStatementPrinter:
  final case class Update(amount: BigDecimal, time: OffsetDateTime)

  private def printUpdates(balance: BigDecimal, updates: List[Update])(using
      console: Console[IO],
  ): IO[Unit] =
    updates match
      case Nil => IO.unit
      case ::(Update(amount, time), next) =>
        val date = time.asString
        console.println(s"$date || $amount || $balance") >> printUpdates(balance - amount, next)

  def impl: AccountStatementPrinter = new AccountStatementPrinter:
    override def print(balance: BigDecimal, updates: List[Update])(using
        console: Console[IO],
    ): IO[Unit] = console.println("Date || Amount || Balance") *> printUpdates(balance, updates)
