package es.eriktorr
package accounts.service

import accounts.domain.Account

import cats.Monad

object AccountService extends Account.Service[Command, Notification]:
  def apply[F[_]: Monad]: App[F, Unit] = App.router {
    case Command.Open =>
      for
        _ <- App.state.decide(_.open)
        accountId <- App.aggregateId
        _ <- App.publish(Notification.AccountOpened(accountId))
      yield ()

    case Command.Deposit(amount) =>
      for
        deposited <- App.state.decide(_.deposit(amount))
        accountId <- App.aggregateId
        _ <- App.publish(Notification.BalanceUpdated(accountId, deposited.balance))
      yield ()

    case Command.Withdraw(amount) =>
      for
        withdrawn <- App.state.decide(_.withdraw(amount))
        accountId <- App.aggregateId
        _ <- App.publish(Notification.BalanceUpdated(accountId, withdrawn.balance))
      yield ()

    case Command.Close => App.state.decide(_.close).void
  }
