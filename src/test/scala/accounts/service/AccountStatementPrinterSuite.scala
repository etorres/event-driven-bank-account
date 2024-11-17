package es.eriktorr
package accounts.service

import accounts.service.AccountStatementPrinter.Update
import accounts.service.AccountStatementPrinterSuite.testScenarioGen
import common.TemporalExtensions.{asString, dateTimeFormatter}
import spec.FakeConsole
import spec.FakeConsole.ConsoleState
import spec.TemporalGenerators.offsetDateTimeGen

import cats.effect.{IO, Ref}
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

import scala.annotation.tailrec

final class AccountStatementPrinterSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should print a bank statement"):
    forAllF(testScenarioGen): testScenario =>
      val testee = AccountStatementPrinter.impl
      (for
        consoleStateRef <- Ref.of[IO, ConsoleState](ConsoleState.empty)
        _ <- testee.print(testScenario.balance, testScenario.updates)(using
          FakeConsole(consoleStateRef),
        )
        obtained <- consoleStateRef.get
      yield obtained.lines).assertEquals(testScenario.expected)

object AccountStatementPrinterSuite:
  final private case class TestScenario(
      balance: BigDecimal,
      expected: List[String],
      updates: List[Update],
  )

  @tailrec
  private def statementFrom(
      accumulated: List[String],
      balance: BigDecimal,
      updates: List[Update],
  ): List[String] =
    updates match
      case Nil => accumulated
      case ::(Update(amount, time), next) =>
        val date = time.asString
        statementFrom(s"$date || $amount || $balance" :: accumulated, balance - amount, next)

  private val testScenarioGen = for
    size <- Gen.choose(3, 7)
    updates <- Gen
      .listOfN(
        size,
        for
          amount <- Gen.choose(-1000d, 1000d).map(BigDecimal.apply)
          time <- offsetDateTimeGen
        yield Update(amount, time),
      )
      .map(_.sortBy(_.time).reverse)
    balance <- Gen.choose(-1000d, 1000d).map(BigDecimal.apply)
    expected = statementFrom(List.empty, balance, updates) ++ List("Date || Amount || Balance")
  yield TestScenario(balance, expected, updates)
