package es.eriktorr
package accounts.domain

import cats.data.ValidatedNec
import cats.implicits.*
import edomata.core.*
import edomata.syntax.all.*
import monocle.syntax.all.*

enum Account:
  case New
  case Open(balance: BigDecimal)
  case Close

  def close: Decision[Rejection, Event, Account] = this
    .perform(mustBeOpen.toDecision.flatMap { account =>
      if account.balance == 0 then Event.Closed.accept
      else Decision.reject(Rejection.NotSettled)
    })

  def deposit(amount: BigDecimal): Decision[Rejection, Event, Open] = this
    .perform(mustBeOpen.toDecision.flatMap { account =>
      if amount > 0 then Decision.accept(Event.Deposited(amount))
      else Decision.reject(Rejection.BadRequest)
    })
    .validate(_.mustBeOpen)

  def open: Decision[Rejection, Event, Open] = this
    .decide {
      case New => Decision.accept(Event.Opened)
      case _ => Decision.reject(Rejection.ExistingAccount)
    }
    .validate(_.mustBeOpen)

  def withdraw(amount: BigDecimal): Decision[Rejection, Event, Open] = this
    .perform(mustBeOpen.toDecision.flatMap { account =>
      if account.balance >= amount && amount > 0 then Decision.accept(Event.Withdrawn(amount))
      else Decision.reject(Rejection.InsufficientBalance)
    })
    .validate(_.mustBeOpen)

  private def mustBeOpen: ValidatedNec[Rejection, Open] = this match
    case open @ Open(_) => open.validNec
    case New => Rejection.NoSuchAccount.invalidNec
    case Close => Rejection.AlreadyClosed.invalidNec

object Account extends DomainModel[Account, Event, Rejection]:
  override def initial: Account = New

  override def transition: Event => Account => ValidatedNec[Rejection, Account] = {
    case Event.Closed => _ => Close.validNec
    case Event.Deposited(amount) => _.mustBeOpen.map(updateBalance(_, amount))
    case Event.Opened => _ => Open(0).validNec
    case Event.Withdrawn(amount) => _.mustBeOpen.map(updateBalance(_, -amount))
  }

  private def updateBalance(account: Account.Open, amount: BigDecimal) =
    account.focus(_.balance).modify(_ + amount)
