package es.eriktorr
package accounts.service

import accounts.domain.{Account, Event, Rejection}
import accounts.service.AccountStatementPrinter.Update

import cats.effect.std.{Console, UUIDGen}
import cats.effect.{Clock, IO, Resource}
import doobie.hikari.HikariTransactor
import edomata.backend.Backend
import edomata.backend.eventsourcing.AggregateState
import edomata.core.CommandMessage
import edomata.doobie.{BackendCodec, CirceCodec, DoobieDriver}

import scala.concurrent.duration.DurationInt

trait AccountServiceFacade:
  def close(accountId: String): IO[Unit]
  def createAccount: IO[String]
  def deposit(accountId: String, amount: BigDecimal): IO[Unit]
  def printStatement(accountId: String, size: Int = 10): IO[Unit]
  def withdraw(accountId: String, amount: BigDecimal): IO[Unit]

object AccountServiceFacade:
  def impl(
      accountStatementPrinter: AccountStatementPrinter,
      transactor: HikariTransactor[IO],
  )(using
      clock: Clock[IO],
      console: Console[IO],
      uuidGen: UUIDGen[IO],
  ): Resource[IO, AccountServiceFacade] =
    val accountService = AccountService[IO]
    val backendResource =
      given BackendCodec[Event] = CirceCodec.jsonb
      given BackendCodec[Notification] = CirceCodec.jsonb
      Backend
        .builder(AccountService)
        .use(DoobieDriver("public", transactor))
        .inMemSnapshot(200)
        .withRetryConfig(maxRetry = 3, retryInitialDelay = 2.seconds)
        .build

    backendResource.map: backend =>
      val service = backend.compile(accountService)
      new AccountServiceFacade:
        override def close(accountId: String): IO[Unit] = for
          commandId <- uuidGen.randomUUID.map(_.toString)
          now <- clock.realTimeInstant
          _ <- service(
            CommandMessage(
              id = commandId,
              time = now,
              address = accountId,
              payload = Command.Close,
            ),
          )
        yield ()

        override def createAccount: IO[String] = for
          accountId <- uuidGen.randomUUID.map(_.toString)
          commandId <- uuidGen.randomUUID.map(_.toString)
          now <- clock.realTimeInstant
          _ <- service(
            CommandMessage(
              id = commandId,
              time = now,
              address = accountId,
              payload = Command.Open,
            ),
          )
        yield accountId

        override def deposit(accountId: String, amount: BigDecimal): IO[Unit] = for
          commandId <- uuidGen.randomUUID.map(_.toString)
          now <- clock.realTimeInstant
          _ <- service(
            CommandMessage(
              id = commandId,
              time = now,
              address = accountId,
              payload = Command.Deposit(amount),
            ),
          )
        yield ()

        override def printStatement(accountId: String, size: Int = 10): IO[Unit] = for
          (currentState, currentVersion) <- backend.repository
            .get(accountId)
            .flatMap:
              case AggregateState.Valid(state, version) => IO.pure((state, version))
              case _ =>
                IO.raiseError(
                  IllegalStateException(s"Conflicting state for account: $accountId"),
                )
          updates <- backend.journal
            .readStreamAfter(accountId, Math.max(0, currentVersion - size - 1))
            .map: event =>
              event.payload match
                case Event.Deposited(amount) => Some(Update(amount, event.metadata.time))
                case Event.Withdrawn(amount) => Some(Update(-amount, event.metadata.time))
                case _ => None
            .collect { case Some(value) => value }
            .compile
            .toList
          _ <- accountStatementPrinter.print(
            currentState match
              case Account.Open(balance) => balance
              case _ => 0
            ,
            updates.reverse,
          )
        yield ()

        override def withdraw(accountId: String, amount: BigDecimal): IO[Unit] = for
          commandId <- uuidGen.randomUUID.map(_.toString)
          now <- clock.realTimeInstant
          _ <- service(
            CommandMessage(
              id = commandId,
              time = now,
              address = accountId,
              payload = Command.Withdraw(amount),
            ),
          )
        yield ()
