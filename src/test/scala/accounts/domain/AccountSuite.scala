package es.eriktorr
package accounts.domain

import edomata.core.Decision
import munit.FunSuite

final class AccountSuite extends FunSuite:
  test("should open an account with default balance"):
    assertEquals(
      Account.New.open: Decision[Rejection, Event, Account],
      Decision.acceptReturn(Account.Open(0))(Event.Opened),
    )

  test("should fail with existing account error when opening a closed account"):
    assertEquals(Account.Close.open, Decision.reject(Rejection.ExistingAccount))

  test("should close an empty account"):
    assertEquals(
      Account.New.open.flatMap(_.close),
      Decision.acceptReturn(Account.Close)(Event.Opened, Event.Closed),
    )

  test("should fail with not settled error when closing a non-empty account"):
    assertEquals(Account.Open(5).close, Decision.reject(Rejection.NotSettled))

  test("should deposit in an account"):
    assertEquals(
      Account.Open(10).deposit(2): Decision[Rejection, Event, Account],
      Decision.acceptReturn(Account.Open(12))(Event.Deposited(2)),
    )

  test("should fail with bad request error when depositing a negative amount"):
    assertEquals(Account.Open(10).deposit(-2), Decision.reject(Rejection.BadRequest))

  test("should withdraw from an account"):
    assertEquals(
      Account.Open(10).withdraw(2): Decision[Rejection, Event, Account],
      Decision.acceptReturn(Account.Open(8))(Event.Withdrawn(2)),
    )

  test("should fail with insufficient balance when there is no enough money in the account"):
    assertEquals(Account.Open(2).withdraw(10), Decision.reject(Rejection.InsufficientBalance))
