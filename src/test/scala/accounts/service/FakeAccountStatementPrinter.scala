package es.eriktorr
package accounts.service

import accounts.service.AccountStatementPrinter.Update
import accounts.service.FakeAccountStatementPrinter.AccountStatementPrinterState

import cats.effect.std.Console
import cats.effect.{IO, Ref}

final class FakeAccountStatementPrinter(stateRef: Ref[IO, AccountStatementPrinterState])
    extends AccountStatementPrinter:
  override def print(balance: BigDecimal, updates: List[Update])(using
      console: Console[IO],
  ): IO[Unit] = stateRef.update(currentState =>
    currentState.copy((balance -> updates) :: currentState.statements),
  )

object FakeAccountStatementPrinter:
  final case class AccountStatementPrinterState(statements: List[(BigDecimal, List[Update])]):
    def set(newStatements: List[(BigDecimal, List[Update])]): AccountStatementPrinterState = copy(
      newStatements,
    )

  object AccountStatementPrinterState:
    val empty: AccountStatementPrinterState = AccountStatementPrinterState(List.empty)
