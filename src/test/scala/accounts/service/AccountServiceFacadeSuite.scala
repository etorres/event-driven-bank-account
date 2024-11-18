package es.eriktorr
package accounts.service

import accounts.service.AccountServiceFacadeSuite.testScenarioGen
import common.application.JdbcTestConfig
import common.db.PostgresTestTransactor
import spec.*
import spec.FakeClock.ClockState
import spec.FakeConsole.ConsoleState
import spec.FakeUUIDGen.UUIDGenState

import cats.effect.{IO, Ref, Resource}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

import java.time.Instant
import java.util.UUID

final class AccountServiceFacadeSuite extends PostgresSuite:
  test("should persist account operations"):
    forAllF(testScenarioGen): testScenario =>
      (for
        transactor <- PostgresTestTransactor(JdbcTestConfig.BackendDatabase).testTransactorResource
        (clockStateRef, consoleStateRef, uuidGenStateRef) <- Resource.eval(
          for
            clockStateRef <- Ref.of[IO, ClockState](ClockState.empty.set(testScenario.instants))
            consoleStateRef <- Ref.of[IO, ConsoleState](ConsoleState.empty)
            uuidGenStateRef <- Ref.of[IO, UUIDGenState](UUIDGenState.empty.set(testScenario.uuids))
          yield (clockStateRef, consoleStateRef, uuidGenStateRef),
        )
        accountStatementPrinter = AccountStatementPrinter.impl
        accountServiceFacade <- AccountServiceFacade.impl(accountStatementPrinter, transactor)(using
          FakeClock(clockStateRef),
          FakeConsole(consoleStateRef),
          FakeUUIDGen(uuidGenStateRef),
        )
      yield (accountServiceFacade, consoleStateRef))
        .use { case (accountServiceFacade, consoleStateRef) =>
          for
            accountId <- accountServiceFacade.createAccount
            _ <- accountServiceFacade.deposit(accountId, 120)
            _ <- accountServiceFacade.withdraw(accountId, 120)
            _ <- accountServiceFacade.close(accountId)
            _ <- accountServiceFacade.printStatement(accountId, 4)
            finalConsoleState <- consoleStateRef.get
          yield accountId
        }
        .assertEquals(testScenario.expected)

object AccountServiceFacadeSuite:
  final private case class TestScenario(
      instants: List[Instant],
      uuids: List[UUID],
      expected: String,
  )

  private val testScenarioGen = for
    instants <- Gen.listOfN(4, TemporalGenerators.instantGen)
    case accountId :: commandIds <- Gen.listOfN(5, Gen.uuid)
  yield TestScenario(instants, accountId :: commandIds, accountId.toString)
