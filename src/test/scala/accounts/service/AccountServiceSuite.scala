package es.eriktorr
package accounts.service

import accounts.domain.{Account, Event}
import accounts.service.AccountServiceSuite.*
import spec.TemporalGenerators.instantGen

import cats.data.{Chain, NonEmptyChain}
import cats.effect.IO
import edomata.core.{CommandMessage, EdomatonResult, RequestContext}
import edomata.syntax.all.*
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

final class AccountServiceSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should close an empty account"):
    testWith(closeTestScenarioGen)

  test("should deposit in an account"):
    testWith(depositTestScenarioGen)

  test("should open a new account"):
    testWith(openTestScenarioGen)

  test("should withdraw from an account"):
    testWith(withdrawTestScenarioGen)

  private def testWith(testScenarioGen: Gen[TestScenario]) =
    forAllF(testScenarioGen): testScenario =>
      val requestContext = RequestContext(testScenario.command, testScenario.state)
      AccountService[IO].execute(requestContext).assertEquals(testScenario.expected)

object AccountServiceSuite:
  final private case class TestScenario(
      command: CommandMessage[Command],
      state: Account,
      expected: EdomatonResult[Account, Event, Nothing, Notification],
  )

  private def commandGen(payload: Command) = for
    accountId <- Gen.uuid.map(_.toString)
    commandId <- Gen.uuid.map(_.toString)
    commandTime <- instantGen
    command = CommandMessage(commandId, commandTime, accountId, payload)
  yield (accountId, command)

  private val closeTestScenarioGen = commandGen(Command.Close).map { case (accountId, command) =>
    TestScenario(
      command,
      Account.Open(0),
      EdomatonResult.Accepted(
        newState = Account.Close,
        events = NonEmptyChain.one(Event.Closed),
        notifications = Chain.empty,
      ),
    )
  }

  private val depositTestScenarioGen = for
    balance <- Gen.choose(100, 1000)
    amount <- Gen.choose(100, 1000)
    (accountId, command) <- commandGen(Command.Deposit(amount))
  yield TestScenario(
    command,
    Account.Open(balance),
    EdomatonResult.Accepted(
      newState = Account.Open(balance + amount),
      events = NonEmptyChain.one(Event.Deposited(amount)),
      notifications = Chain.one(Notification.BalanceUpdated(accountId, balance + amount)),
    ),
  )

  private val openTestScenarioGen = commandGen(Command.Open).map { case (accountId, command) =>
    TestScenario(
      command,
      Account.New,
      EdomatonResult.Accepted(
        newState = Account.Open(0),
        events = NonEmptyChain.one(Event.Opened),
        notifications = Chain.one(Notification.AccountOpened(accountId)),
      ),
    )
  }

  private val withdrawTestScenarioGen = for
    balance <- Gen.choose(100, 1000)
    amount <- Gen.choose(100, balance)
    (accountId, command) <- commandGen(Command.Withdraw(amount))
  yield TestScenario(
    command,
    Account.Open(balance),
    EdomatonResult.Accepted(
      newState = Account.Open(balance - amount),
      events = NonEmptyChain.one(Event.Withdrawn(amount)),
      notifications = Chain.one(Notification.BalanceUpdated(accountId, balance - amount)),
    ),
  )
