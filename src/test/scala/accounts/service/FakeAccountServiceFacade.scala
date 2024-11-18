package es.eriktorr
package accounts.service

import accounts.service.FakeAccountServiceFacade.{AccountServiceFacadeState, FakeAccount}

import cats.effect.std.UUIDGen
import cats.effect.{IO, Ref}

final class FakeAccountServiceFacade(stateRef: Ref[IO, AccountServiceFacadeState])(using
    uuidGen: UUIDGen[IO],
) extends AccountServiceFacade:
  override def close(accountId: String): IO[Unit] =
    stateRef.update(currentState =>
      currentState.copy(currentState.accounts.filterNot { case (id, _) => id == accountId }),
    )

  override def createAccount: IO[String] = for
    accountId <- uuidGen.randomUUID.map(_.toString)
    _ <- stateRef.update(currentState =>
      currentState.copy(currentState.accounts + (accountId -> FakeAccount(0d, List.empty))),
    )
  yield accountId

  override def deposit(accountId: String, amount: BigDecimal): IO[Unit] =
    IO.raiseError(IllegalArgumentException("not implemented"))

  override def printStatement(accountId: String, size: Int): IO[Unit] =
    IO.raiseError(IllegalArgumentException("not implemented"))

  override def withdraw(accountId: String, amount: BigDecimal): IO[Unit] =
    IO.raiseError(IllegalArgumentException("not implemented"))

object FakeAccountServiceFacade:
  final case class FakeOperation(amount: BigDecimal)

  final case class FakeAccount(balance: BigDecimal, operations: List[FakeOperation])

  final case class AccountServiceFacadeState(accounts: Map[String, FakeAccount]):
    def set(newAccounts: Map[String, FakeAccount]): AccountServiceFacadeState = copy(newAccounts)

  object AccountServiceFacadeState:
    val empty: AccountServiceFacadeState = AccountServiceFacadeState(Map.empty)
